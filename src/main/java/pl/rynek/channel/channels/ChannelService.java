package pl.rynek.channel.channels;

import java.time.Clock;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.api.dto.CreateChannelRequest;
import pl.rynek.channel.api.dto.SetBusinessDetailsRequest;
import pl.rynek.channel.api.dto.UpdateChannelRequest;
import pl.rynek.channel.identity.IdentityClient;

@Service
public class ChannelService {

    private final ChannelRepository channels;
    private final IdentityClient identity;
    private final Clock clock;

    public ChannelService(ChannelRepository channels, IdentityClient identity, Clock clock) {
        this.channels = channels;
        this.identity = identity;
        this.clock = clock;
    }

    /**
     * Kanał zapisuje się przed wywołaniem identity (konflikt handle daje 409 bez śladu w identity), a błąd identity
     * wycofuje transakcję. Jeśli zatwierdzenie zawiedzie już po odpowiedzi identity, konto wskaże nieistniejący
     * kanał; to zniknie razem z wywołaniem synchronicznym, gdy zastąpi je zdarzenie {@code channel.created}.
     */
    @Transactional
    public Channel create(UUID ownerId, CreateChannelRequest request) {
        String handle = Handles.parse(request.handle());
        if (channels.existsByOwnerId(ownerId)) {
            throw alreadyHasChannel();
        }
        if (channels.existsByHandle(handle)) {
            throw handleTaken();
        }
        Channel channel = new Channel(ownerId, handle, request.name().trim(),
                request.description() == null ? "" : request.description().trim(), clock.instant());
        channels.save(channel);
        flushOrConflict();
        identity.linkChannel(ownerId, channel.getId(), handle);
        return channel;
    }

    @Transactional(readOnly = true)
    public Channel get(String channelId) {
        UUID id;
        try {
            id = UUID.fromString(channelId);
        } catch (IllegalArgumentException e) {
            throw notFound();
        }
        return channels.findById(id).orElseThrow(ChannelService::notFound);
    }

    @Transactional(readOnly = true)
    public Channel getByHandle(String handle) {
        return channels.findByHandle(Handles.normalize(handle)).orElseThrow(ChannelService::notFound);
    }

    @Transactional
    public Channel update(UUID userId, String channelId, UpdateChannelRequest request) {
        Channel channel = owned(userId, channelId);
        if (request.name() != null) {
            channel.rename(request.name().trim(), clock.instant());
        }
        if (request.description() != null) {
            channel.describe(request.description().trim(), clock.instant());
        }
        if (request.phone() != null) {
            channel.changeContactPhone(blankToNull(request.phone()), clock.instant());
        }
        if (request.email() != null) {
            channel.changeContactEmail(blankToNull(request.email()), clock.instant());
        }
        return channel;
    }

    @Transactional
    public Channel setBusinessDetails(UUID userId, String channelId, SetBusinessDetailsRequest request) {
        Channel channel = owned(userId, channelId);
        channel.changeBusinessDetails(request.companyName().trim(), Nip.parse(request.nip()),
                request.registeredAddress().trim(), clock.instant());
        return channel;
    }

    /** Kanał zalogowanego sprzedawcy; cudzy kanał daje 403 (endpointy {@code seller}). */
    @Transactional(readOnly = true)
    public Channel owned(UUID userId, String channelId) {
        Channel channel = get(channelId);
        if (!channel.isOwnedBy(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Możesz zmieniać tylko własny kanał.");
        }
        return channel;
    }

    /** Dwa równoległe żądania mogą oba przejść sprawdzenie {@code exists...}; wtedy rozstrzyga unikalny indeks. */
    private void flushOrConflict() {
        try {
            channels.flush();
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            throw message.contains("channels_handle_key") ? handleTaken() : alreadyHasChannel();
        }
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie znaleźliśmy tego kanału.");
    }

    private static ResponseStatusException handleTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ten adres kanału jest już zajęty. Wybierz inny.");
    }

    private static ResponseStatusException alreadyHasChannel() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Masz już kanał. Jedno konto może mieć jeden kanał.");
    }
}

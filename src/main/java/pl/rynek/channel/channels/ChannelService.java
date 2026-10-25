package pl.rynek.channel.channels;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.api.dto.CreateChannelRequest;
import pl.rynek.channel.api.dto.SetBusinessDetailsRequest;
import pl.rynek.channel.api.dto.UpdateChannelRequest;
import pl.rynek.channel.identity.IdentityClient;
import pl.rynek.channel.media.MediaClient;
import pl.rynek.channel.media.MediaClient.MediaFile;

@Service
public class ChannelService {

    private final ChannelRepository channels;
    private final IdentityClient identity;
    private final MediaClient media;
    private final JdbcClient jdbc;
    private final Clock clock;

    public ChannelService(ChannelRepository channels, IdentityClient identity, MediaClient media, JdbcClient jdbc, Clock clock) {
        this.channels = channels;
        this.identity = identity;
        this.media = media;
        this.jdbc = jdbc;
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

    @Transactional
    public Channel setAvatar(UUID userId, String channelId, UUID mediaId) {
        Channel channel = owned(userId, channelId);
        channel.changeAvatar(ownFile(userId, mediaId, "AVATAR").url(), clock.instant());
        return channel;
    }

    @Transactional
    public Channel setBanner(UUID userId, String channelId, UUID mediaId) {
        Channel channel = owned(userId, channelId);
        channel.changeBanner(ownFile(userId, mediaId, "BANNER").url(), clock.instant());
        return channel;
    }

    /**
     * Zapisuje dokumenty i ustawia {@code PENDING}. Wymaga danych firmy, bo weryfikacja polega na porównaniu
     * dokumentów z NIP-em i nazwą. Ocena dokumentów (VERIFIED/REJECTED) należy do panelu admina.
     */
    @Transactional
    public Channel requestVerification(UUID userId, String channelId, List<UUID> documentMediaIds) {
        Channel channel = owned(userId, channelId);
        if (channel.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ten kanał jest już zweryfikowany.");
        }
        if (!channel.hasBusinessDetails()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Najpierw uzupełnij dane firmy: nazwę, NIP i adres rejestrowy.");
        }
        Instant now = clock.instant();
        for (UUID mediaId : documentMediaIds.stream().distinct().toList()) {
            ownFile(userId, mediaId, "KYC_DOCUMENT");
            jdbc.sql("INSERT INTO verification_documents (channel_id, media_id, submitted_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING")
                    .params(channel.getId(), mediaId, Timestamp.from(now))
                    .update();
        }
        channel.submitForVerification(now);
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

    /** Plik musi należeć do właściciela kanału i mieć właściwe przeznaczenie, inaczej można by przypiąć cudze zdjęcie. */
    private MediaFile ownFile(UUID userId, UUID mediaId, String purpose) {
        MediaFile file = media.get(mediaId);
        if (!file.ownerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Możesz użyć tylko plików, które sam wysłałeś.");
        }
        if (!file.purpose().equals(purpose)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten plik wysłano w innym celu. Wyślij go ponownie.");
        }
        return file;
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

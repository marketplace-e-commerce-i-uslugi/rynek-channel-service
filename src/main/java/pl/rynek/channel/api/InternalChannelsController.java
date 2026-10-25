package pl.rynek.channel.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.api.dto.ChannelRefResponse;
import pl.rynek.channel.api.dto.OwnerChannelResponse;
import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.channels.ChannelRepository;
import pl.rynek.channel.channels.ChannelService;

@RestController
public class InternalChannelsController {

    /** Tyle kart ofert mieści się w jednej stronie wyników z zapasem. */
    private static final int MAX_BATCH = 100;

    private final ChannelService channelService;
    private final ChannelRepository channels;

    public InternalChannelsController(ChannelService channelService, ChannelRepository channels) {
        this.channelService = channelService;
        this.channels = channels;
    }

    /**
     * Dane kanału dla innych serwisów. Dostęp: int.
     */
    @GetMapping("/internal/channels/{channelId}")
    public ChannelRefResponse getChannel(@PathVariable String channelId) {
        return ChannelRefResponse.of(channelService.get(channelId));
    }

    /**
     * Kanał konta (catalog przy tworzeniu oferty). Dostęp: int. Konto bez kanału daje 404.
     */
    @GetMapping("/internal/channels/by-owner/{userId}")
    public OwnerChannelResponse getChannelByOwner(@PathVariable String userId) {
        UUID owner = uuidOrNull(userId);
        return (owner == null ? Optional.<Channel>empty() : channels.findByOwnerId(owner))
                .map(OwnerChannelResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To konto nie ma kanału."));
    }

    /**
     * Wsadowo (karty ofert). Dostęp: int. {@code ids} rozdzielone przecinkami; nieznane i niepoprawne są pomijane,
     * kolejność wyniku jak w {@code ids}.
     */
    @GetMapping("/internal/channels")
    public List<ChannelRefResponse> getChannelsBatch(@RequestParam(required = false) String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        List<UUID> requested = Arrays.stream(ids.split(",")).map(String::trim).map(InternalChannelsController::uuidOrNull)
                .filter(Objects::nonNull).distinct().toList();
        if (requested.size() > MAX_BATCH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Najwyżej " + MAX_BATCH + " kanałów w jednym zapytaniu.");
        }
        Map<UUID, Channel> found = channels.findAllById(requested).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));
        return requested.stream().filter(found::containsKey).map(id -> ChannelRefResponse.of(found.get(id))).toList();
    }

    private static UUID uuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

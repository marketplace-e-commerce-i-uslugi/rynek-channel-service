package pl.rynek.channel.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.api.dto.ChannelRefResponse;
import pl.rynek.channel.api.dto.OffersCountRequest;
import pl.rynek.channel.api.dto.OwnerChannelResponse;
import pl.rynek.channel.api.dto.ChannelSearchItem;
import pl.rynek.channel.api.dto.ChannelSearchPage;
import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.channels.ChannelRepository;
import pl.rynek.channel.channels.ChannelService;
import pl.rynek.channel.follow.FollowRepository;

@RestController
public class InternalChannelsController {

    /** Tyle kart ofert mieści się w jednej stronie wyników z zapasem. */
    private static final int MAX_BATCH = 100;
    static final int MAX_SYNC = 200;

    private final ChannelService channelService;
    private final ChannelRepository channels;
    private final FollowRepository follows;

    public InternalChannelsController(ChannelService channelService, ChannelRepository channels, FollowRepository follows) {
        this.channelService = channelService;
        this.channels = channels;
        this.follows = follows;
    }

    /**
     * Wszystkie kanały po kolei (po id rosnąco, {@code afterId} to ostatni id poprzedniej strony, {@code limit} najwyżej
     * {@value #MAX_SYNC}) do indeksu wyszukiwarki. Pełny przegląd zamiast zdarzeń {@code channel.*}: liczba obserwujących i
     * weryfikacja zmieniają się bez zmiany kanału, więc nie da się ich wychwycić po czasie edycji. Dostęp: int.
     */
    @GetMapping("/internal/channels/search-sync")
    public ChannelSearchPage searchSync(@RequestParam(required = false) String afterId, @RequestParam(defaultValue = "100") int limit) {
        UUID after = afterId == null || afterId.isBlank() ? new UUID(0, 0) : uuidOrNull(afterId);
        if (after == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nieprawidłowy kursor synchronizacji.");
        }
        int size = Math.max(1, Math.min(limit, MAX_SYNC));
        List<Channel> page = channels.findPageAfter(after, PageRequest.of(0, size));
        Map<UUID, Long> followers = new HashMap<>();
        if (!page.isEmpty()) {
            follows.countByChannelIds(page.stream().map(Channel::getId).toList()).forEach(row -> followers.put((UUID) row[0], (Long) row[1]));
        }
        List<ChannelSearchItem> items = page.stream().map(c -> ChannelSearchItem.of(c, followers.getOrDefault(c.getId(), 0L))).toList();
        return new ChannelSearchPage(items, page.size() == size ? page.getLast().getId().toString() : null);
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

    /**
     * Liczba opublikowanych ofert kanału (catalog po publikacji, wstrzymaniu i usunięciu). Dostęp: int.
     * Zastępuje zdarzenia {@code offer.published}/{@code offer.unpublished}, dopóki nie ma Kafki.
     */
    @PutMapping("/internal/channels/{channelId}/offers-count")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setOffersCount(@PathVariable String channelId, @Valid @RequestBody OffersCountRequest request) {
        channelService.setOffersCount(channelId, request.count());
    }

    private static UUID uuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

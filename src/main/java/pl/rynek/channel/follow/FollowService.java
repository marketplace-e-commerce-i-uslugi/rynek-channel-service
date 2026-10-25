package pl.rynek.channel.follow;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.channels.ChannelRepository;
import pl.rynek.channel.channels.ChannelService;

/** Obserwuj / przestań obserwować są idempotentne: powtórzenie daje ten sam wynik bez błędu. */
@Service
public class FollowService {

    /** Liczba obserwujących i stan obserwowania dla oglądającego ({@code null}, gdy niezalogowany). */
    public record Stats(long followersCount, Boolean following, Boolean notificationsOn) {
    }

    /** Pozycja listy „Obserwowane”: kanał i ustawienie dzwonka. */
    public record Followed(Channel channel, boolean notificationsOn) {
    }

    private final FollowRepository follows;
    private final ChannelRepository channelRepository;
    private final ChannelService channels;
    private final Clock clock;

    public FollowService(FollowRepository follows, ChannelRepository channelRepository, ChannelService channels, Clock clock) {
        this.follows = follows;
        this.channelRepository = channelRepository;
        this.channels = channels;
        this.clock = clock;
    }

    @Transactional
    public void follow(UUID userId, String channelId) {
        Channel channel = channels.get(channelId);
        if (channel.isOwnedBy(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nie możesz obserwować własnego kanału.");
        }
        follows.insertIfAbsent(userId, channel.getId(), clock.instant());
    }

    @Transactional
    public void unfollow(UUID userId, String channelId) {
        follows.deleteByKey(userId, channels.get(channelId).getId());
    }

    @Transactional
    public void setNotifications(UUID userId, String channelId, boolean on) {
        if (follows.updateNotifications(userId, channels.get(channelId).getId(), on) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie obserwujesz tego kanału.");
        }
    }

    @Transactional(readOnly = true)
    public Stats stats(Channel channel, Optional<UUID> viewer) {
        long followers = follows.countByChannelId(channel.getId());
        if (viewer.isEmpty()) {
            return new Stats(followers, null, null);
        }
        Optional<Follow> follow = follows.findByUserIdAndChannelId(viewer.get(), channel.getId());
        return new Stats(followers, follow.isPresent(), follow.map(Follow::isNotificationsOn).orElse(false));
    }

    /** Od ostatnio zaobserwowanego. */
    @Transactional(readOnly = true)
    public List<Followed> following(UUID userId) {
        List<Follow> list = follows.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Channel> byId = channelRepository.findAllById(list.stream().map(Follow::getChannelId).toList()).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity()));
        return list.stream()
                .filter(f -> byId.containsKey(f.getChannelId()))
                .map(f -> new Followed(byId.get(f.getChannelId()), f.isNotificationsOn()))
                .toList();
    }
}

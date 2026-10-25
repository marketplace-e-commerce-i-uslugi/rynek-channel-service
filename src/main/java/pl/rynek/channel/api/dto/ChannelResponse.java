package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.follow.FollowService;

/**
 * Kanał. Kontrakt: {@code Channel} w rynek-frontend/src/app/core/api/models/channel.ts.
 * {@code offersCount} należy do catalog-service, a {@code rating} do engagement-service (zdarzenie {@code review.created});
 * do czasu integracji są zerowe. {@code following}/{@code notificationsOn} są puste dla niezalogowanych.
 */
public record ChannelResponse(
        String id,
        String handle,
        String name,
        String description,
        String avatarUrl,
        String bannerUrl,
        boolean verified,
        long followersCount,
        long offersCount,
        Rating rating,
        Boolean following,
        Boolean notificationsOn) {

    public record Rating(double average, long count) {
    }

    public static ChannelResponse of(Channel channel, FollowService.Stats stats) {
        return new ChannelResponse(
                channel.getId().toString(),
                channel.getHandle(),
                channel.getName(),
                channel.getDescription(),
                channel.getAvatarUrl(),
                channel.getBannerUrl(),
                channel.isVerified(),
                stats.followersCount(),
                0,
                new Rating(0, 0),
                stats.following(),
                stats.notificationsOn());
    }
}

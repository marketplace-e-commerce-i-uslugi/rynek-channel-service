package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.follow.FollowService;

/**
 * Kanał. Kontrakt: {@code Channel} w rynek-frontend/src/app/core/api/models/channel.ts.
 * {@code offersCount} (opublikowane oferty) ustawia catalog-service. {@code rating} liczy engagement-service
 * (pobierany przy odczycie, bez zdarzenia {@code review.created}); gdy nie odpowiada, jest zerowy. {@code following}/{@code notificationsOn} są puste dla niezalogowanych.
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

    public static ChannelResponse of(Channel channel, FollowService.Stats stats, Rating rating) {
        return new ChannelResponse(
                channel.getId().toString(),
                channel.getHandle(),
                channel.getName(),
                channel.getDescription(),
                channel.getAvatarUrl(),
                channel.getBannerUrl(),
                channel.isVerified(),
                stats.followersCount(),
                channel.getOffersCount(),
                rating,
                stats.following(),
                stats.notificationsOn());
    }
}

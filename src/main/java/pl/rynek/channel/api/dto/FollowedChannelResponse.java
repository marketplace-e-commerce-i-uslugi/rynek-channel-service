package pl.rynek.channel.api.dto;

import pl.rynek.channel.follow.FollowService;

/**
 * Pozycja listy „Obserwowane”. Kontrakt: {@code FollowedChannel} w rynek-frontend/src/app/core/api/models/channel.ts.
 */
public record FollowedChannelResponse(String id, String handle, String name, String avatarUrl, boolean notificationsOn) {

    public static FollowedChannelResponse of(FollowService.Followed followed) {
        return new FollowedChannelResponse(
                followed.channel().getId().toString(),
                followed.channel().getHandle(),
                followed.channel().getName(),
                followed.channel().getAvatarUrl(),
                followed.notificationsOn());
    }
}

package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;

/**
 * Skrócone dane kanału dla innych serwisów (karty ofert, czat, koszyk). Kontrakt: {@code ChannelRef}
 * w rynek-frontend/src/app/core/api/models/common.ts.
 */
public record ChannelRefResponse(String id, String handle, String name, String avatarUrl, boolean verified) {

    public static ChannelRefResponse of(Channel channel) {
        return new ChannelRefResponse(channel.getId().toString(), channel.getHandle(), channel.getName(),
                channel.getAvatarUrl(), channel.isVerified());
    }
}

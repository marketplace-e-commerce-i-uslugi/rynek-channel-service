package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;

/** Kanał sprzedawcy dla catalog-service przy tworzeniu oferty: dane karty i adres jako domyślna lokalizacja. */
public record OwnerChannelResponse(String id, String handle, String name, String avatarUrl, boolean verified, String address) {

    public static OwnerChannelResponse of(Channel channel) {
        return new OwnerChannelResponse(channel.getId().toString(), channel.getHandle(), channel.getName(),
                channel.getAvatarUrl(), channel.isVerified(), channel.getAddress());
    }
}

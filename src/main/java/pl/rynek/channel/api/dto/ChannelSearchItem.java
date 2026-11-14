package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;

/**
 * Kanał w indeksie wyszukiwarki ({@code GET /internal/channels/search-sync}): dane z {@code ChannelRef} plus opis, liczba
 * opublikowanych ofert i obserwujących.
 */
public record ChannelSearchItem(String id, String handle, String name, String avatarUrl, boolean verified, String description,
                                int offersCount, long followersCount) {

    public static ChannelSearchItem of(Channel channel, long followers) {
        return new ChannelSearchItem(channel.getId().toString(), channel.getHandle(), channel.getName(), channel.getAvatarUrl(),
                channel.isVerified(), channel.getDescription(), channel.getOffersCount(), followers);
    }
}

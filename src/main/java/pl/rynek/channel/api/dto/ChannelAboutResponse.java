package pl.rynek.channel.api.dto;

import java.time.Instant;

import pl.rynek.channel.channels.Channel;

/**
 * Zakładka „O nas”. Kontrakt: {@code ChannelAbout} w rynek-frontend/src/app/core/api/models/channel.ts.
 * {@code address} i {@code openingHours} nie mają jeszcze endpointu do edycji, więc są puste.
 */
public record ChannelAboutResponse(
        String description,
        String address,
        String openingHours,
        String phone,
        String email,
        BusinessDetailsResponse company,
        Instant joinedAt) {

    public static ChannelAboutResponse of(Channel channel) {
        return new ChannelAboutResponse(
                channel.getDescription(),
                channel.getAddress(),
                channel.getOpeningHours(),
                channel.getContactPhone(),
                channel.getContactEmail(),
                BusinessDetailsResponse.of(channel),
                channel.getCreatedAt());
    }
}

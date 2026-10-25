package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.Channel;

/**
 * Dane firmy. Kontrakt: {@code BusinessDetails} w rynek-frontend/src/app/core/api/models/channel.ts. NIP jako 10 cyfr.
 */
public record BusinessDetailsResponse(String companyName, String nip, String registeredAddress) {

    /** @return {@code null}, gdy sprzedawca nie podał danych firmy */
    public static BusinessDetailsResponse of(Channel channel) {
        if (!channel.hasBusinessDetails()) {
            return null;
        }
        return new BusinessDetailsResponse(channel.getCompanyName(), channel.getNip(), channel.getRegisteredAddress());
    }
}

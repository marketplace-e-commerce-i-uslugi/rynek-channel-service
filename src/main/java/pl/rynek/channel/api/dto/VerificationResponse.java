package pl.rynek.channel.api.dto;

import pl.rynek.channel.channels.VerificationStatus;

/**
 * Status weryfikacji: {@code {status}} jak w {@code ChannelApi.getVerificationStatus} w rynek-frontend.
 */
public record VerificationResponse(VerificationStatus status) {
}

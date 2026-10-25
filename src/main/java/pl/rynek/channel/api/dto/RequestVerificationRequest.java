package pl.rynek.channel.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Pliki z media-service o przeznaczeniu {@code KYC_DOCUMENT} (np. odpis z KRS lub CEIDG). */
public record RequestVerificationRequest(
        @NotEmpty(message = "Dołącz co najmniej jeden dokument.")
        @Size(max = 5, message = "Możesz dołączyć najwyżej 5 dokumentów.")
        List<@NotNull UUID> documentMediaIds) {
}

package pl.rynek.channel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ciało żądania: Utworzenie kanału (po wyborze roli usługodawca/merchant). Kontrakt: {@code CreateChannelRequest}
 * w rynek-frontend/src/app/core/api/models/channel.ts. Format handle sprawdza {@code Handles}.
 */
public record CreateChannelRequest(
        @NotBlank(message = "Podaj nazwę kanału.") @Size(max = 80, message = "Nazwa kanału może mieć najwyżej 80 znaków.") String name,
        @NotBlank(message = "Podaj adres kanału.") String handle,
        @Size(max = 2000, message = "Opis może mieć najwyżej 2000 znaków.") String description) {
}

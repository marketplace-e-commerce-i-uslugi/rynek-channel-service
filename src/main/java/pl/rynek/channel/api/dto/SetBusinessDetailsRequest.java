package pl.rynek.channel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ciało żądania: NIP, nazwa firmy, adres rejestrowy. Kontrakt: {@code BusinessDetails} w
 * rynek-frontend/src/app/core/api/models/channel.ts. Poprawność NIP sprawdza {@code Nip}.
 */
public record SetBusinessDetailsRequest(
        @NotBlank(message = "Podaj nazwę firmy.") @Size(max = 200, message = "Nazwa firmy może mieć najwyżej 200 znaków.") String companyName,
        @NotBlank(message = "Podaj NIP.") String nip,
        @NotBlank(message = "Podaj adres rejestrowy.") @Size(max = 300, message = "Adres rejestrowy może mieć najwyżej 300 znaków.") String registeredAddress) {
}

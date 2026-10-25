package pl.rynek.channel.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ciało żądania: Edycja nazwy, opisu, danych kontaktowych. Pola pominięte (null) zostają bez zmian;
 * pusty telefon lub e-mail usuwa dane kontaktowe.
 */
public record UpdateChannelRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Podaj nazwę kanału.") @Size(max = 80, message = "Nazwa kanału może mieć najwyżej 80 znaków.") String name,
        @Size(max = 2000, message = "Opis może mieć najwyżej 2000 znaków.") String description,
        @Pattern(regexp = "[+0-9 ()-]{0,20}", message = "Podaj poprawny numer telefonu.") String phone,
        @Email(message = "Podaj poprawny adres e-mail.") @Size(max = 254, message = "Adres e-mail jest za długi.") String email) {
}

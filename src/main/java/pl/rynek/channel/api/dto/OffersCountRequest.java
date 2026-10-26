package pl.rynek.channel.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Liczba opublikowanych ofert kanału wyliczona przez catalog-service. */
public record OffersCountRequest(
        @NotNull(message = "Podaj liczbę ofert.")
        @Min(value = 0, message = "Liczba ofert nie może być ujemna.")
        Integer count) {
}

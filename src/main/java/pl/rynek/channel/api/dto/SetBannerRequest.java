package pl.rynek.channel.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Plik z media-service o przeznaczeniu {@code BANNER}, wysłany przez właściciela kanału. */
public record SetBannerRequest(
        @NotNull(message = "Wybierz zdjęcie.")
        UUID mediaId) {
}

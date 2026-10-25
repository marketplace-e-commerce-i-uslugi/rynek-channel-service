package pl.rynek.channel.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Plik z media-service o przeznaczeniu {@code AVATAR}, wysłany przez właściciela kanału. */
public record SetAvatarRequest(
        @NotNull(message = "Wybierz zdjęcie.")
        UUID mediaId) {
}

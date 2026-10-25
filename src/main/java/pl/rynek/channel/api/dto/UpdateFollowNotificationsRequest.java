package pl.rynek.channel.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Ciało żądania: Włącz/wyłącz powiadomienia (dzwonek).
 */
public record UpdateFollowNotificationsRequest(@NotNull(message = "Podaj, czy powiadomienia mają być włączone.") Boolean notificationsOn) {
}

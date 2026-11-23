package pl.rynek.channel.notification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Powiadomienia w dzwonku (notification-service, {@code POST /internal/notifications}) zamiast zdarzeń Kafka.
 * Best effort: wysyłka idzie w tle i nigdy nie blokuje ani nie psuje operacji, której dotyczy. Przy braku odpowiedzi
 * próbuje jeszcze dwa razy; {@code dedupeKey} sprawia, że ponowienie nie tworzy drugiego powiadomienia.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final int ATTEMPTS = 3;
    private static final long PAUSE_MS = 2000;

    private final RestClient http;

    public NotificationClient(@Value("${rynek.notification.url}") String notificationUrl) {
        org.springframework.http.client.JdkClientHttpRequestFactory requests = new org.springframework.http.client.JdkClientHttpRequestFactory();
        requests.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.http = RestClient.builder().baseUrl(notificationUrl).requestFactory(requests).build();
    }

    /** {@code category}: {@code messages}, {@code orders}, {@code bookings} albo {@code followed}; {@code link}: ścieżka w aplikacji. */
    public void send(UUID userId, String category, String title, String body, String link, String dedupeKey) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", userId);
        request.put("category", category);
        request.put("title", title);
        request.put("body", body);
        if (link != null) {
            request.put("link", link);
        }
        if (dedupeKey != null) {
            request.put("dedupeKey", dedupeKey);
        }
        Thread.startVirtualThread(() -> post(request));
    }

    private void post(Map<String, Object> request) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                http.post().uri("/internal/notifications").body(request).retrieve().toBodilessEntity();
                return;
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().is4xxClientError()) {
                    log.warn("notification-service odrzucił powiadomienie {}: {}", request.get("dedupeKey"), e.getStatusCode());
                    return;
                }
            } catch (ResourceAccessException e) {
                log.debug("Brak odpowiedzi notification-service (próba {}): {}", attempt, e.getMessage());
            }
            if (attempt < ATTEMPTS) {
                try {
                    Thread.sleep(PAUSE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("Nie udało się wysłać powiadomienia {} do notification-service", request.get("dedupeKey"));
    }
}

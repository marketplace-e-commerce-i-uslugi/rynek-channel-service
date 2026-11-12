package pl.rynek.channel.engagement;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Ocena kanału z engagement-service ({@code GET /internal/ratings}). Bez engagement kanał ma ocenę 0 / 0. */
@Component
public class RatingClient {

    private static final Logger log = LoggerFactory.getLogger(RatingClient.class);

    private final RestClient http;

    public RatingClient(@Value("${rynek.engagement.url}") String engagementUrl) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory();
        requests.setReadTimeout(Duration.ofSeconds(2));
        this.http = RestClient.builder().baseUrl(engagementUrl).requestFactory(requests).build();
    }

    public Rating of(UUID channelId) {
        try {
            List<Rating> found = http.get().uri(uri -> uri.path("/internal/ratings").queryParam("channelIds", channelId).build())
                    .retrieve().body(new ParameterizedTypeReference<>() {
                    });
            return found == null || found.isEmpty() ? Rating.NONE : found.getFirst();
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.warn("Brak oceny kanału {}: {}", channelId, e.getMessage());
            return Rating.NONE;
        }
    }

    public record Rating(UUID channelId, double average, long count) {
        public static final Rating NONE = new Rating(null, 0, 0);
    }
}

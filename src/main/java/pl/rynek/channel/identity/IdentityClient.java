package pl.rynek.channel.identity;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Wywołania {@code /internal/...} identity-service. Tymczasowo zastępuje zdarzenie {@code channel.created}
 * (§17), dopóki nie ma Kafki: identity zapisuje kanał przy koncie, więc {@code GET /users/me} zwraca {@code channelId}.
 */
@Component
public class IdentityClient {

    private final RestClient http;

    public IdentityClient(@Value("${rynek.identity.url}") String identityUrl) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory();
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().baseUrl(identityUrl).requestFactory(requests).build();
    }

    /**
     * Przypina kanał do konta. identity sprawdza, czy konto ma rolę usługodawcy lub merchanta i czy nie ma już
     * innego kanału; jego 403/404/409 przechodzi do klienta z tym samym komunikatem.
     */
    public void linkChannel(UUID userId, UUID channelId, String handle) {
        try {
            http.put()
                    .uri("/internal/users/{userId}/channel", userId)
                    .body(Map.of("channelId", channelId.toString(), "channelHandle", handle))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw forwarded(e);
        } catch (ResourceAccessException e) {
            throw unavailable();
        }
    }

    private static ResponseStatusException forwarded(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.value() != 403 && status.value() != 404 && status.value() != 409) {
            return unavailable();
        }
        ProblemDetail problem = e.getResponseBodyAs(ProblemDetail.class);
        String detail = problem != null && problem.getDetail() != null ? problem.getDetail() : "Nie udało się utworzyć kanału.";
        return new ResponseStatusException(status, detail);
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serwis kont jest niedostępny. Spróbuj ponownie za chwilę.");
    }
}

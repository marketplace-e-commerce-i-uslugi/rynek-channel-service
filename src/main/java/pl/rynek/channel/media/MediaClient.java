package pl.rynek.channel.media;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Odczyt plików z media-service ({@code /internal/media/{id}}) przed przypięciem ich do kanału. */
@Component
public class MediaClient {

    private final RestClient http;

    public MediaClient(@Value("${rynek.media.url}") String mediaUrl) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory();
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().baseUrl(mediaUrl).requestFactory(requests).build();
    }

    /** Gotowy plik; nieistniejący lub niepotwierdzony daje 400, bo to błąd w danych żądania. */
    public MediaFile get(UUID mediaId) {
        try {
            return http.get().uri("/internal/media/{mediaId}", mediaId).retrieve().body(MediaFile.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie znaleźliśmy tego pliku. Wyślij go ponownie.");
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serwis plików jest niedostępny. Spróbuj ponownie za chwilę.");
        }
    }

    /** {@code url} jest pusty dla plików prywatnych (dokumenty weryfikacji). */
    public record MediaFile(UUID id, UUID ownerId, String purpose, String contentType, String url) {
    }
}

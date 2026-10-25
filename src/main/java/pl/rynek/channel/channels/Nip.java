package pl.rynek.channel.channels;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** NIP: 10 cyfr z cyfrą kontrolną (suma ważona modulo 11). Separatory ("525-000-00-00", spacje) są pomijane. */
public final class Nip {

    private static final int[] WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};

    private Nip() {
    }

    public static String parse(String raw) {
        String digits = raw.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NIP musi mieć 10 cyfr.");
        }
        if (!checksumMatches(digits)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten NIP jest nieprawidłowy. Sprawdź, czy nie ma literówki.");
        }
        return digits;
    }

    private static boolean checksumMatches(String digits) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += WEIGHTS[i] * (digits.charAt(i) - '0');
        }
        return sum % 11 == digits.charAt(9) - '0';
    }
}

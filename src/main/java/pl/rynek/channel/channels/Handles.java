package pl.rynek.channel.channels;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Handle kanału ({@code @elektromarket}): zapisywany bez "@" i małymi literami, więc wielkość liter nie tworzy duplikatów. */
public final class Handles {

    /** 3–30 znaków, litera lub cyfra na początku i końcu. */
    private static final Pattern HANDLE = Pattern.compile("[a-z0-9][a-z0-9._-]{1,28}[a-z0-9]");

    private Handles() {
    }

    public static String normalize(String raw) {
        String handle = raw.trim();
        if (handle.startsWith("@")) {
            handle = handle.substring(1);
        }
        return handle.toLowerCase(Locale.ROOT);
    }

    /** Jak {@link #normalize}, a nieprawidłowy handle daje 400. */
    public static String parse(String raw) {
        String handle = normalize(raw);
        if (!HANDLE.matcher(handle).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Adres kanału musi mieć od 3 do 30 znaków: litery bez polskich znaków, cyfry, kropka, myślnik lub podkreślnik.");
        }
        return handle;
    }
}

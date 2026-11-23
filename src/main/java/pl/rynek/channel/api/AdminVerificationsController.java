package pl.rynek.channel.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.channels.ChannelService;
import pl.rynek.channel.media.MediaClient;
import pl.rynek.channel.notification.NotificationClient;

/** Wnioski o weryfikację kanałów (dokumenty sprzedawcy). Dostęp: admin (rola {@code ADMIN} w tokenie, nadawana poza aplikacją). */
@RestController
public class AdminVerificationsController {

    private final ChannelService channels;
    private final MediaClient media;
    private final NotificationClient notifications;

    public AdminVerificationsController(ChannelService channels, MediaClient media, NotificationClient notifications) {
        this.channels = channels;
        this.media = media;
        this.notifications = notifications;
    }

    /**
     * Kanały czekające na decyzję, od najstarszego: dane firmy do porównania z dokumentami i krótko ważne adresy plików.
     */
    @GetMapping("/api/v1/admin/channel-verifications")
    public List<Application> list(@AuthenticationPrincipal Jwt token) {
        requireAdmin(token);
        return channels.pendingVerifications().stream().map(this::view).toList();
    }

    /** Zatwierdzenie: kanał dostaje znak weryfikacji. 409, gdy kanał nie czeka na decyzję. */
    @PostMapping("/api/v1/admin/channel-verifications/{channelId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@AuthenticationPrincipal Jwt token, @PathVariable String channelId) {
        requireAdmin(token);
        notify(channels.decideVerification(channelId, true), true);
    }

    /** Odrzucenie: sprzedawca może zgłosić dokumenty ponownie. */
    @PostMapping("/api/v1/admin/channel-verifications/{channelId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal Jwt token, @PathVariable String channelId) {
        requireAdmin(token);
        notify(channels.decideVerification(channelId, false), false);
    }

    /** Sprzedawca dowiaduje się o decyzji z dzwonka; link prowadzi do ekranu kanału. */
    private void notify(Channel channel, boolean approved) {
        String key = "verification-" + channel.getId() + "-" + (approved ? "approved" : "rejected") + "-" + System.currentTimeMillis();
        if (approved) {
            notifications.send(channel.getOwnerId(), "messages", "Kanał zweryfikowany", "Kanał „" + channel.getName() + "” ma teraz znak weryfikacji.", "/sprzedawca/kanal", key);
        } else {
            notifications.send(channel.getOwnerId(), "messages", "Wniosek o weryfikację odrzucony", "Sprawdź dokumenty kanału „" + channel.getName() + "” i zgłoś je ponownie.", "/sprzedawca/kanal", key);
        }
    }

    private Application view(Channel c) {
        List<Document> documents = channels.verificationDocuments(c.getId()).stream()
                .map(d -> new Document(d.mediaId(), media.signedUrl(d.mediaId()), d.submittedAt())).toList();
        return new Application(c.getId(), c.getHandle(), c.getName(), c.getCompanyName(), c.getNip(), c.getRegisteredAddress(), documents);
    }

    private static void requireAdmin(Jwt token) {
        List<String> roles = token.getClaimAsStringList("roles");
        if (roles == null || !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ta operacja wymaga uprawnień administratora.");
        }
    }

    public record Document(UUID mediaId, String url, Instant submittedAt) {
    }

    public record Application(UUID channelId, String handle, String name, String companyName, String nip, String registeredAddress,
                              List<Document> documents) {
    }
}

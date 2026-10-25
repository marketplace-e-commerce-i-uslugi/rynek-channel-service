package pl.rynek.channel.api;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.channel.api.dto.BusinessDetailsResponse;
import pl.rynek.channel.api.dto.ChannelAboutResponse;
import pl.rynek.channel.api.dto.ChannelResponse;
import pl.rynek.channel.api.dto.CreateChannelRequest;
import pl.rynek.channel.api.dto.RequestVerificationRequest;
import pl.rynek.channel.api.dto.SetAvatarRequest;
import pl.rynek.channel.api.dto.SetBannerRequest;
import pl.rynek.channel.api.dto.SetBusinessDetailsRequest;
import pl.rynek.channel.api.dto.UpdateChannelRequest;
import pl.rynek.channel.api.dto.UpdateFollowNotificationsRequest;
import pl.rynek.channel.api.dto.VerificationResponse;
import pl.rynek.channel.channels.Channel;
import pl.rynek.channel.channels.ChannelService;
import pl.rynek.channel.config.Viewer;
import pl.rynek.channel.follow.FollowService;

import static pl.rynek.channel.common.NotImplemented.notImplemented;

@RestController
public class ChannelsController {

    private final ChannelService channels;
    private final FollowService follows;
    private final Viewer viewer;

    public ChannelsController(ChannelService channels, FollowService follows, Viewer viewer) {
        this.channels = channels;
        this.follows = follows;
        this.viewer = viewer;
    }

    /**
     * Utworzenie kanału (po wyborze roli usługodawca/merchant). Dostęp: user.
     */
    @PostMapping("/api/v1/channels")
    public ResponseEntity<ChannelResponse> createChannel(@AuthenticationPrincipal Jwt token,
                                                         @Valid @RequestBody CreateChannelRequest request) {
        UUID userId = userId(token);
        Channel channel = channels.create(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/channels/" + channel.getId()))
                .body(ChannelResponse.of(channel, follows.stats(channel, Optional.of(userId))));
    }

    /**
     * Dane kanału: nazwa, handle, opis, liczba obserwujących, liczba ofert, ocena, status weryfikacji. Dostęp: pub.
     */
    @GetMapping("/api/v1/channels/{channelId}")
    public ChannelResponse getChannel(@PathVariable String channelId, HttpServletRequest http) {
        return view(channels.get(channelId), http);
    }

    /**
     * Kanał po handle (@elektromarket). Dostęp: pub.
     */
    @GetMapping("/api/v1/channels/by-handle/{handle}")
    public ChannelResponse getChannelByHandle(@PathVariable String handle, HttpServletRequest http) {
        return view(channels.getByHandle(handle), http);
    }

    /**
     * Edycja nazwy, opisu, danych kontaktowych. Dostęp: seller.
     */
    @PatchMapping("/api/v1/channels/{channelId}")
    public ChannelResponse updateChannel(@AuthenticationPrincipal Jwt token, @PathVariable String channelId,
                                         @Valid @RequestBody UpdateChannelRequest request) {
        UUID userId = userId(token);
        Channel channel = channels.update(userId, channelId, request);
        return ChannelResponse.of(channel, follows.stats(channel, Optional.of(userId)));
    }

    /**
     * Ustawienie awatara (mediaId z media-service). Dostęp: seller.
     */
    @PutMapping("/api/v1/channels/{channelId}/avatar")
    public void setAvatar(@PathVariable String channelId, @RequestBody SetAvatarRequest request) {
        throw notImplemented();
    }

    /**
     * Ustawienie banera (mediaId). Dostęp: seller.
     */
    @PutMapping("/api/v1/channels/{channelId}/banner")
    public void setBanner(@PathVariable String channelId, @RequestBody SetBannerRequest request) {
        throw notImplemented();
    }

    /**
     * Zakładka „O nas”: opis, adres, godziny, dane firmy. Dostęp: pub.
     */
    @GetMapping("/api/v1/channels/{channelId}/about")
    public ChannelAboutResponse getAbout(@PathVariable String channelId) {
        return ChannelAboutResponse.of(channels.get(channelId));
    }

    /**
     * NIP, nazwa firmy, adres rejestrowy. Dostęp: seller.
     */
    @PutMapping("/api/v1/channels/{channelId}/business-details")
    public BusinessDetailsResponse setBusinessDetails(@AuthenticationPrincipal Jwt token, @PathVariable String channelId,
                                                      @Valid @RequestBody SetBusinessDetailsRequest request) {
        return BusinessDetailsResponse.of(channels.setBusinessDetails(userId(token), channelId, request));
    }

    /**
     * Zgłoszenie weryfikacji (dokumenty). Dostęp: seller.
     */
    @PostMapping("/api/v1/channels/{channelId}/verification")
    public void requestVerification(@PathVariable String channelId, @RequestBody RequestVerificationRequest request) {
        throw notImplemented();
    }

    /**
     * Status weryfikacji. Dostęp: seller.
     */
    @GetMapping("/api/v1/channels/{channelId}/verification")
    public VerificationResponse getVerificationStatus(@AuthenticationPrincipal Jwt token, @PathVariable String channelId) {
        return new VerificationResponse(channels.owned(userId(token), channelId).getVerificationStatus());
    }

    /**
     * Obserwuj. Dostęp: user.
     */
    @PostMapping("/api/v1/channels/{channelId}/follow")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal Jwt token, @PathVariable String channelId) {
        follows.follow(userId(token), channelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Przestań obserwować. Dostęp: user.
     */
    @DeleteMapping("/api/v1/channels/{channelId}/follow")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal Jwt token, @PathVariable String channelId) {
        follows.unfollow(userId(token), channelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Włącz/wyłącz powiadomienia (dzwonek). Dostęp: user.
     */
    @PatchMapping("/api/v1/channels/{channelId}/follow")
    public ResponseEntity<Void> updateFollowNotifications(@AuthenticationPrincipal Jwt token, @PathVariable String channelId,
                                                          @Valid @RequestBody UpdateFollowNotificationsRequest request) {
        follows.setNotifications(userId(token), channelId, request.notificationsOn());
        return ResponseEntity.noContent().build();
    }

    private ChannelResponse view(Channel channel, HttpServletRequest http) {
        return ChannelResponse.of(channel, follows.stats(channel, viewer.of(http)));
    }

    private static UUID userId(Jwt token) {
        return UUID.fromString(token.getSubject());
    }
}

package pl.rynek.channel.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.channel.api.dto.CreateChannelRequest;
import pl.rynek.channel.api.dto.RequestVerificationRequest;
import pl.rynek.channel.api.dto.SetAvatarRequest;
import pl.rynek.channel.api.dto.SetBannerRequest;
import pl.rynek.channel.api.dto.SetBusinessDetailsRequest;
import pl.rynek.channel.api.dto.UpdateChannelRequest;
import pl.rynek.channel.api.dto.UpdateFollowNotificationsRequest;

import static pl.rynek.channel.common.NotImplemented.notImplemented;

@RestController
public class ChannelsController {

    /**
     * Utworzenie kanału (po wyborze roli usługodawca/merchant). Dostęp: user.
     */
    @PostMapping("/api/v1/channels")
    public void createChannel(@RequestBody CreateChannelRequest request) {
        throw notImplemented();
    }

    /**
     * Dane kanału: nazwa, handle, opis, liczba obserwujących, liczba ofert, ocena, status weryfikacji. Dostęp: pub.
     */
    @GetMapping("/api/v1/channels/{channelId}")
    public void getChannel(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * Kanał po handle (@elektromarket). Dostęp: pub.
     */
    @GetMapping("/api/v1/channels/by-handle/{handle}")
    public void getChannelByHandle(@PathVariable String handle) {
        throw notImplemented();
    }

    /**
     * Edycja nazwy, opisu, danych kontaktowych. Dostęp: seller.
     */
    @PatchMapping("/api/v1/channels/{channelId}")
    public void updateChannel(@PathVariable String channelId, @RequestBody UpdateChannelRequest request) {
        throw notImplemented();
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
    public void getAbout(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * NIP, nazwa firmy, adres rejestrowy. Dostęp: seller.
     */
    @PutMapping("/api/v1/channels/{channelId}/business-details")
    public void setBusinessDetails(@PathVariable String channelId, @RequestBody SetBusinessDetailsRequest request) {
        throw notImplemented();
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
    public void getVerificationStatus(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * Obserwuj. Dostęp: user.
     */
    @PostMapping("/api/v1/channels/{channelId}/follow")
    public void follow(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * Przestań obserwować. Dostęp: user.
     */
    @DeleteMapping("/api/v1/channels/{channelId}/follow")
    public void unfollow(@PathVariable String channelId) {
        throw notImplemented();
    }

    /**
     * Włącz/wyłącz powiadomienia (dzwonek). Dostęp: user.
     */
    @PatchMapping("/api/v1/channels/{channelId}/follow")
    public void updateFollowNotifications(@PathVariable String channelId, @RequestBody UpdateFollowNotificationsRequest request) {
        throw notImplemented();
    }
}

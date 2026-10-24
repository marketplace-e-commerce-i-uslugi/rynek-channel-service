package pl.rynek.channel.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static pl.rynek.channel.common.NotImplemented.notImplemented;

@RestController
public class UsersController {

    /**
     * Lista obserwowanych kanałów (boczne menu, „Obserwowane”). Dostęp: user.
     */
    @GetMapping("/api/v1/users/me/following")
    public void listFollowedChannels() {
        throw notImplemented();
    }
}

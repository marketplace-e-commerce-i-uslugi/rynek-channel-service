package pl.rynek.channel.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.channel.api.dto.FollowedChannelResponse;
import pl.rynek.channel.follow.FollowService;

@RestController
public class UsersController {

    private final FollowService follows;

    public UsersController(FollowService follows) {
        this.follows = follows;
    }

    /**
     * Lista obserwowanych kanałów (boczne menu, „Obserwowane”). Dostęp: user.
     * Bez paginacji: frontend oczekuje tablicy ({@code FollowedChannel[]}).
     */
    @GetMapping("/api/v1/users/me/following")
    public List<FollowedChannelResponse> listFollowedChannels(@AuthenticationPrincipal Jwt token) {
        return follows.following(UUID.fromString(token.getSubject())).stream().map(FollowedChannelResponse::of).toList();
    }
}

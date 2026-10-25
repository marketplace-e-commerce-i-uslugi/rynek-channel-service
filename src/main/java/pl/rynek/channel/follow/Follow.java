package pl.rynek.channel.follow;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Obserwowanie kanału przez użytkownika; dzwonek ({@code notificationsOn}) domyślnie wyłączony. */
@Entity
@Table(name = "follows")
@IdClass(Follow.Key.class)
public class Follow {

    public record Key(UUID userId, UUID channelId) implements Serializable {
    }

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "notifications_on")
    private boolean notificationsOn;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Follow() {
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public boolean isNotificationsOn() {
        return notificationsOn;
    }
}

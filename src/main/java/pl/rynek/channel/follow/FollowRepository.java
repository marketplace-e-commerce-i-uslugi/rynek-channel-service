package pl.rynek.channel.follow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepository extends JpaRepository<Follow, Follow.Key> {

    long countByChannelId(UUID channelId);

    Optional<Follow> findByUserIdAndChannelId(UUID userId, UUID channelId);

    List<Follow> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Ponowne „Obserwuj” (także równoległe) niczego nie zmienia, w tym ustawienia dzwonka. */
    @Modifying
    @Query(value = "INSERT INTO follows (user_id, channel_id, notifications_on, created_at) VALUES (:userId, :channelId, false, :now) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertIfAbsent(UUID userId, UUID channelId, Instant now);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.userId = :userId AND f.channelId = :channelId")
    void deleteByKey(UUID userId, UUID channelId);

    /** @return liczba zmienionych wierszy; 0 = użytkownik nie obserwuje kanału */
    @Modifying
    @Query("UPDATE Follow f SET f.notificationsOn = :on WHERE f.userId = :userId AND f.channelId = :channelId")
    int updateNotifications(UUID userId, UUID channelId, boolean on);
}

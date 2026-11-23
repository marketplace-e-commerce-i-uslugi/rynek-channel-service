package pl.rynek.channel.channels;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findByHandle(String handle);

    /** Wnioski o weryfikację, od najstarszego. */
    @Query("select c from Channel c where c.verificationStatus = pl.rynek.channel.channels.VerificationStatus.PENDING order by c.updatedAt, c.id")
    List<Channel> findPendingVerification();

    boolean existsByHandle(String handle);

    boolean existsByOwnerId(UUID ownerId);

    Optional<Channel> findByOwnerId(UUID ownerId);

    /** Kanały po id rosnąco, od następnego po {@code afterId} (indeks wyszukiwarki). */
    @Query("select c from Channel c where c.id > :afterId order by c.id")
    List<Channel> findPageAfter(UUID afterId, Pageable page);
}

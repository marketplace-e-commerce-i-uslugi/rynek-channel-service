package pl.rynek.channel.channels;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    Optional<Channel> findByHandle(String handle);

    boolean existsByHandle(String handle);

    boolean existsByOwnerId(UUID ownerId);
}

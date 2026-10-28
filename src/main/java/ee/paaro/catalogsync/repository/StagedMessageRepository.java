package ee.paaro.catalogsync.repository;

import ee.paaro.catalogsync.domain.StagedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StagedMessageRepository extends JpaRepository<StagedMessage, Long> {

    List<StagedMessage> findAllByUnresolvedKeyAndStatus(String unresolvedKey, StagedMessage.Status status);

    List<StagedMessage> findAllByStatusOrderByReceivedAtAsc(StagedMessage.Status status);

    long countByStatus(StagedMessage.Status status);
}

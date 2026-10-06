package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.ai.EmbeddingRecord;
import trademind.tm_server.invest.ai.EmbeddingSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmbeddingRepository extends JpaRepository<EmbeddingRecord, Long> {
    Optional<EmbeddingRecord> findBySourceTypeAndSourceIdAndModel(EmbeddingSource type, Long sourceId, String model);

    List<EmbeddingRecord> findByModel(String model);

    void deleteBySourceTypeAndSourceId(EmbeddingSource type, Long sourceId);
}

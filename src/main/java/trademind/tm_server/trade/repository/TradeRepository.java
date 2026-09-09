package trademind.tm_server.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.trade.Trade;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByDeletedAtIsNull();

    Optional<Trade> findByIdAndDeletedAtIsNull(Long id);

    // Batch dedupe check for imports: one query for the whole incoming batch
    // instead of an existence check per trade.
    List<Trade> findByExternalIdIn(Collection<String> externalIds);
}
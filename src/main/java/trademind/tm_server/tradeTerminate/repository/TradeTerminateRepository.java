package trademind.tm_server.tradeTerminate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeTerminate.TradeTerminate;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeTerminateRepository extends JpaRepository<TradeTerminate, Long> {
    List<TradeTerminate> findByDeletedAtIsNull();

    Optional<TradeTerminate> findByIdAndDeletedAtIsNull(Long id);
}
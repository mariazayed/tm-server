package trademind.tm_server.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.trade.Trade;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByDeletedAtIsNull();

    Optional<Trade> findByIdAndDeletedAtIsNull(Long id);
}
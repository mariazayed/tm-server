package trademind.tm_server.tradeInsight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeInsight.TradeInsight;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeInsightRepository extends JpaRepository<TradeInsight, Long> {
    List<TradeInsight> findByDeletedAtIsNull();

    Optional<TradeInsight> findByIdAndDeletedAtIsNull(Long id);

    List<TradeInsight> findByTradeIdAndDeletedAtIsNull(Long tradeId);
}

package trademind.tm_server.tradePlan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradePlan.TradePlan;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradePlanRepository extends JpaRepository<TradePlan, Long> {
    List<TradePlan> findByDeletedAtIsNull();

    Optional<TradePlan> findByIdAndDeletedAtIsNull(Long id);
}
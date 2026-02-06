package trademind.tm_server.strategy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.strategy.Strategy;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    List<Strategy> findByDeletedAtIsNull();

    Optional<Strategy> findByIdAndDeletedAtIsNull(Long id);
}
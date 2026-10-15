package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.Holding;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /** Live funds in the order they were added; the first is the primary one. */
    List<Holding> findByDeletedAtIsNullOrderByIdAsc();

    Optional<Holding> findByIdAndDeletedAtIsNull(Long id);

    Optional<Holding> findByTickerIgnoreCaseAndDeletedAtIsNull(String ticker);
}

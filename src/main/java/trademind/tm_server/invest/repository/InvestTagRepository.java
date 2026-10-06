package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.InvestTag;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestTagRepository extends JpaRepository<InvestTag, Long> {
    List<InvestTag> findByDeletedAtIsNullOrderByNameAsc();

    Optional<InvestTag> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}

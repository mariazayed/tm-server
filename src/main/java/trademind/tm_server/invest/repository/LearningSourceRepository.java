package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.LearningSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningSourceRepository extends JpaRepository<LearningSource, Long> {
    List<LearningSource> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<LearningSource> findByIdAndDeletedAtIsNull(Long id);
}

package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.LearningNote;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningNoteRepository extends JpaRepository<LearningNote, Long> {
    List<LearningNote> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<LearningNote> findByIdAndDeletedAtIsNull(Long id);

    List<LearningNote> findByDeletedAtIsNullAndSource_IdOrderByCreatedAtDesc(Long sourceId);
}

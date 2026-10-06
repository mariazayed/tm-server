package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.LearningNote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningNoteRepository
        extends JpaRepository<LearningNote, Long>, JpaSpecificationExecutor<LearningNote> {

    List<LearningNote> findByDeletedAtIsNullOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"tags", "source"})
    Optional<LearningNote> findByIdAndDeletedAtIsNull(Long id);

    /** Loads tags and source in one query; used to hydrate an already-paged list of ids. */
    @EntityGraph(attributePaths = {"tags", "source"})
    List<LearningNote> findByIdIn(Collection<Long> ids);

    List<LearningNote> findByDeletedAtIsNullAndSource_IdOrderByCreatedAtDesc(Long sourceId);
}

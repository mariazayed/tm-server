package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.JournalEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository
        extends JpaRepository<JournalEntry, Long>, JpaSpecificationExecutor<JournalEntry> {

    List<JournalEntry> findByDeletedAtIsNullOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "tags")
    Optional<JournalEntry> findByIdAndDeletedAtIsNull(Long id);

    /** Loads the tags in the same query; used to hydrate an already-paged list of ids. */
    @EntityGraph(attributePaths = "tags")
    List<JournalEntry> findByIdIn(Collection<Long> ids);

    List<JournalEntry> findByDeletedAtIsNullAndTags_IdOrderByCreatedAtDesc(Long tagId);
}

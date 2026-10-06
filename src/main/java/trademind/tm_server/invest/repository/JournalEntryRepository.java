package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.JournalEntry;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<JournalEntry> findByIdAndDeletedAtIsNull(Long id);

    List<JournalEntry> findByDeletedAtIsNullAndTags_IdOrderByCreatedAtDesc(Long tagId);
}

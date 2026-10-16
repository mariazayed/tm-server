package trademind.tm_server.reminderNote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trademind.tm_server.reminderNote.ReminderNoteCategory;

import java.util.List;
import java.util.Optional;

public interface ReminderNoteCategoryRepository extends JpaRepository<ReminderNoteCategory, Long> {
    List<ReminderNoteCategory> findByDeletedAtIsNullOrderByNameAsc();
    Optional<ReminderNoteCategory> findByIdAndDeletedAtIsNull(Long id);
}

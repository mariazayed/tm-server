package trademind.tm_server.reminderNote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.reminderNote.ReminderNote;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface ReminderNoteRepository extends JpaRepository<ReminderNote, Long> {
    List<ReminderNote> findByDeletedAtIsNull();

    Optional<ReminderNote> findByIdAndDeletedAtIsNull(Long id);
}
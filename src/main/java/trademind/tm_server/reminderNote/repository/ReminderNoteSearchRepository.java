package trademind.tm_server.reminderNote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.reminderNote.ReminderNote;

// For dynamic search queries

@Repository
public interface ReminderNoteSearchRepository extends JpaRepository<ReminderNote, Long>, JpaSpecificationExecutor<ReminderNote> {
}

package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.reminder.ReminderLog;

import java.util.List;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {
    List<ReminderLog> findByMonth(String month);

    List<ReminderLog> findTop5ByOrderBySentAtDesc();
}

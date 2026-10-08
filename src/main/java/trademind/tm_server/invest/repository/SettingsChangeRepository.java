package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.SettingsChange;

import java.util.List;

@Repository
public interface SettingsChangeRepository extends JpaRepository<SettingsChange, Long> {

    List<SettingsChange> findByDeletedAtIsNullOrderByCreatedAtAscIdAsc();
}

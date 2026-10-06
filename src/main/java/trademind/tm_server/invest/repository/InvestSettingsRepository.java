package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.InvestSettings;


@Repository
public interface InvestSettingsRepository extends JpaRepository<InvestSettings, Long> {

}

package trademind.tm_server.memoir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.memoir.Memoir;

// For dynamic search queries

@Repository
public interface MemoirSearchRepository extends JpaRepository<Memoir, Long>, JpaSpecificationExecutor<Memoir> {
}

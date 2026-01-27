package trademind.tm_server.memoir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.memoir.Memoir;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface MemoirRepository extends JpaRepository<Memoir, Long> {
    List<Memoir> findByDeletedAtIsNull();

    Optional<Memoir> findByIdAndDeletedAtIsNull(Long id);
}
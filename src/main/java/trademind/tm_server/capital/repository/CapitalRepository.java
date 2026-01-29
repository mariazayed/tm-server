package trademind.tm_server.capital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.capital.Capital;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface CapitalRepository extends JpaRepository<Capital, Long> {
    List<Capital> findByDeletedAtIsNull();

    Optional<Capital> findByIdAndDeletedAtIsNull(Long id);
}
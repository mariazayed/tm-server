package trademind.tm_server.day.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.day.Day;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface DayRepository extends JpaRepository<Day, Long> {
    List<Day> findByDeletedAtIsNull();

    Optional<Day> findByIdAndDeletedAtIsNull(Long id);
}
package trademind.tm_server.invest.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.Flashcard;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    Optional<Flashcard> findByIdAndDeletedAtIsNull(Long id);

    List<Flashcard> findByDeletedAtIsNullOrderByDueOnAscIdAsc();

    List<Flashcard> findByDeletedAtIsNullAndNote_IdOrderByIdAsc(Long noteId);

    /** Cards due on or before [day], most overdue first. */
    List<Flashcard> findByDeletedAtIsNullAndDueOnLessThanEqualOrderByDueOnAscIdAsc(LocalDate day, Pageable limit);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndDueOnLessThanEqual(LocalDate day);

    long countByDeletedAtIsNullAndLastReviewedOn(LocalDate day);

    long countByDeletedAtIsNullAndLastReviewedOnIsNull();
}

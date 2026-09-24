package trademind.tm_server.questionCategory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trademind.tm_server.questionCategory.QuestionCategory;

import java.util.List;
import java.util.Optional;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, Long> {
    List<QuestionCategory> findByDeletedAtIsNull();

    Optional<QuestionCategory> findByIdAndDeletedAtIsNull(Long id);
}

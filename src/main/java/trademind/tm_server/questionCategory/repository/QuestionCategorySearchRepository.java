package trademind.tm_server.questionCategory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import trademind.tm_server.questionCategory.QuestionCategory;

public interface QuestionCategorySearchRepository extends JpaRepository<QuestionCategory, Long>, JpaSpecificationExecutor<QuestionCategory> {
}

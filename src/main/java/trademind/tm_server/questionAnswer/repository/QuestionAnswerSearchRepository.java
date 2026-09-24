package trademind.tm_server.questionAnswer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import trademind.tm_server.questionAnswer.QuestionAnswer;

public interface QuestionAnswerSearchRepository extends JpaRepository<QuestionAnswer, Long>, JpaSpecificationExecutor<QuestionAnswer> {
}

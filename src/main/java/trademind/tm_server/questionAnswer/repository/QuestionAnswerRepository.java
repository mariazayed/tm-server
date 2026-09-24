package trademind.tm_server.questionAnswer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trademind.tm_server.questionAnswer.QuestionAnswer;

import java.util.List;
import java.util.Optional;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findByDeletedAtIsNull();

    Optional<QuestionAnswer> findByIdAndDeletedAtIsNull(Long id);
}

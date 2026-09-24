package trademind.tm_server.questionCategory;

import org.mapstruct.Mapper;
import trademind.tm_server.questionCategory.dto.NewQuestionCategoryRequest;
import trademind.tm_server.questionCategory.dto.QuestionCategoryResponse;

@Mapper(componentModel = "spring")
public interface QuestionCategoryMapper {
    QuestionCategoryResponse toResponse(QuestionCategory questionCategory);

    QuestionCategory toEntity(NewQuestionCategoryRequest request);
}

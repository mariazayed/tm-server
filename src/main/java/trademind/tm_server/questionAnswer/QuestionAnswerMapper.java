package trademind.tm_server.questionAnswer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import trademind.tm_server.questionAnswer.dto.NewQuestionAnswerRequest;
import trademind.tm_server.questionAnswer.dto.QuestionAnswerResponse;
import trademind.tm_server.questionAnswer.dto.UpdateQuestionAnswerRequest;
import trademind.tm_server.questionCategory.QuestionCategoryMapper;

@Mapper(componentModel = "spring", uses = QuestionCategoryMapper.class)
public interface QuestionAnswerMapper {
    QuestionAnswerResponse toResponse(QuestionAnswer questionAnswer);

    @Mapping(target = "category", ignore = true)
    QuestionAnswer toEntity(NewQuestionAnswerRequest request);

    @Mapping(target = "category", ignore = true)
    void updateEntity(UpdateQuestionAnswerRequest dto, @MappingTarget QuestionAnswer entity);
}

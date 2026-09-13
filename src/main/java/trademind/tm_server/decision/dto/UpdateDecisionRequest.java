package trademind.tm_server.decision.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import trademind.tm_server.enums.DecisionCategoryEnum;
import trademind.tm_server.enums.DecisionStatusEnum;

import java.time.LocalDateTime;

@Data
public class UpdateDecisionRequest {
    // Matches the space-separated format every *Model in the Flutter app
    // sends (e.g. GoalModel, MemoirModel) -- Jackson's default LocalDateTime
    // deserializer only accepts the ISO 'T' separator otherwise.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime decisionDate;
    private String title;
    private DecisionCategoryEnum category;
    private DecisionStatusEnum status;

    // Markdown text from the Flutter app's MdEditorField. No length limit --
    // stored as `text` on Decision.
    private String description;
    private String reasoning;

    private Integer relatedDecisionNumber;
    private String notes;

    // decisionNumber is intentionally NOT here -- it's assigned once on
    // create and never changes (MapStruct's updateEntity only touches
    // fields present on this DTO, so leaving it out makes it immutable).
}

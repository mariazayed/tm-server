package trademind.tm_server.decision.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import trademind.tm_server.enums.DecisionCategoryEnum;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NewDecisionRequest {
    // Matches the space-separated format every *Model in the Flutter app
    // sends (e.g. GoalModel, MemoirModel) -- Jackson's default LocalDateTime
    // deserializer only accepts the ISO 'T' separator otherwise.
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime decisionDate;
    private String title;
    private DecisionCategoryEnum category;

    // Markdown text from the Flutter app's MdEditorField. No length limit --
    // stored as `text` on Decision.
    private String description;
    private String reasoning;

    private List<Integer> relatedDecisionNumbers;
    private String notes;

    // decisionNumber is intentionally NOT here -- it's server-assigned in
    // DecisionService.create() (see AppConstants/Decision.decisionNumber),
    // never taken from the client.
}

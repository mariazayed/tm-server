package trademind.tm_server.tradeEvaluate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.ScoreEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTradeEvaluateRequest {
    /// ********** Execution Feedback **********
    @NotNull(message = "Required")
    private boolean wasThePlanFollowed;

    @NotNull(message = "Required")
    private String whatWentWrong;

    @NotNull(message = "Required")
    private String whatShouldImproveNextTime;

    /// ********** Performance Scores **********
    @NotNull(message = "Required")
    private ScoreEnum analysisAccuracy;

    @NotNull(message = "Required")
    private ScoreEnum setupQuality;

    @NotNull(message = "Required")
    private ScoreEnum entryTiming;

    @NotNull(message = "Required")
    private ScoreEnum discipline;

    @NotNull(message = "Required")
    private ScoreEnum emotionalState;

    @NotNull(message = "Required")
    private ScoreEnum riskManagement;

    private String notes;
}

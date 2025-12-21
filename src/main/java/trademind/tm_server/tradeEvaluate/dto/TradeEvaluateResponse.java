package trademind.tm_server.tradeEvaluate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trademind.tm_server.enums.ScoreEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvaluateResponse {
    /// ********** Execution Feedback **********
    private boolean wasThePlanFollowed;
    private String whatWentWrong;
    private String whatShouldImproveNextTime;

    /// ********** Performance Scores **********
    private ScoreEnum analysisAccuracy;
    private ScoreEnum setupQuality;
    private ScoreEnum entryTiming;
    private ScoreEnum discipline;
    private ScoreEnum emotionalState;
    private ScoreEnum riskManagement;

    private String notes;
}

package trademind.tm_server.tradeEvaluate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.ScoreEnum;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_EVALUATIONS)
public class TradeEvaluate extends BaseEntity {
    /// ********** Execution Feedback **********
    @Column(nullable = false)
    private boolean wasThePlanFollowed;

    @Column(nullable = false)
    private String whatWentWrong;

    @Column(nullable = false)
    private String whatShouldImproveNextTime;

    /// ********** Performance Scores **********
    @Column(nullable = false)
    private ScoreEnum analysisAccuracy;

    @Column(nullable = false)
    private ScoreEnum setupQuality;

    @Column(nullable = false)
    private ScoreEnum entryTiming;

    @Column(nullable = false)
    private ScoreEnum discipline;

    @Column(nullable = false)
    private ScoreEnum emotionalState;

    @Column(nullable = false)
    private ScoreEnum riskManagement;
}
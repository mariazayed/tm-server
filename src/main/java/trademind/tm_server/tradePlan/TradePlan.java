package trademind.tm_server.tradePlan;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_PLANS)
public class TradePlan extends BaseEntity {

    @Column(nullable = false)
    private TradeDirectionEnum direction;

    @Column(nullable = false)
    private MarketBiasEnum marketBias;

    @Column(nullable = false)
    private MarketStateEnum marketState;

    @Column(nullable = false)
    private List<EntryReasonEnum> entryReasons;

    @Column(nullable = false)
    private List<EntryConfirmationEnum> entryConfirmations;

    @Column(nullable = false)
    private EntryZoneTypeEnum entryZoneType;

    @Column(nullable = false)
    private double entryZoneStartLevel;

    @Column(nullable = false)
    private double entryZoneEndLevel;

    @Column(nullable = false)
    private double invalidationStartLevel;

    @Column(nullable = false)
    private double invalidationEndLevel;

    @Column(nullable = false)
    private InvalidationConditionEnum invalidationCondition;

    @Column(nullable = false)
    private boolean willPyramid;

    @Column(nullable = false)
    private List<ScalingRuleEnum> scalingRules;

    @Column(nullable = false)
    private ProbabilityEnum expectedProbabilityOfSuccess;

    @Column(nullable = false)
    private boolean willBoost;

    @Column(nullable = false)
    private List<BoostRuleEnum> boostRules;

    @Column(nullable = false)
    private VolatilityEnum volatilityExpectation;
}
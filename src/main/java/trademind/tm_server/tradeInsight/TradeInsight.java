package trademind.tm_server.tradeInsight;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.Trade;

import java.time.LocalDateTime;

// AI-generated narrative review of a trade, kept separate from the trade's own
// data so it's versionable and never silently overwrites a person's own notes
// or confirmed fields. A trade can accumulate more than one insight over time
// (re-run after a prompt change) rather than being overwritten in place.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_INSIGHTS)
public class TradeInsight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String promptVersion;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String suggestedImprovement;

    // AI-detected candidates, distinct from the person-confirmed
    // Trade.mistakeTags collection -- kept as a JSON-encoded string, not the
    // MistakeTagEnum or a native array column, since these are exploratory
    // suggestions rather than validated data, and it sidesteps any ambiguity
    // in how a given Hibernate version maps array columns under
    // ddl-auto=validate. e.g. '["REVENGE_TRADE","FOMO_ENTRY"]'
    @Column(columnDefinition = "text")
    private String detectedMistakeTags;

    private Double confidence;

    @Column(nullable = false)
    private LocalDateTime generatedAt;
}

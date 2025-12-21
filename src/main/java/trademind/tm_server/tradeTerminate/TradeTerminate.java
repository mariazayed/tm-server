package trademind.tm_server.tradeTerminate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.ExitReasonEnum;
import trademind.tm_server.trade.TradeSteps;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_TERMINATIONS)
public class TradeTerminate extends BaseEntity {
    @Column(nullable = false)
    private LocalDateTime exitedAt;

    @Column(nullable = false)
    private double exitPrice;

    @Column(nullable = false)
    private boolean earlyExit;

    @Column(nullable = false)
    private List<ExitReasonEnum> exitReasons;

    @OneToOne(mappedBy = "tradeTermination")
    private TradeSteps tradeSteps;
}
package trademind.tm_server.tradeMonitor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_MONITORING)
public class TradeMonitor extends BaseEntity {
    @Column(nullable = false)
    private String marketBehaviorAfterEntry;

    @Column(nullable = false)
    private boolean anyPlanDeviation;

    @Column(nullable = false)
    private String updates;
}
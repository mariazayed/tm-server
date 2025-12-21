package trademind.tm_server.trade;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.tradeExecution.TradeExecution;
import trademind.tm_server.tradeMonitor.TradeMonitor;
import trademind.tm_server.tradeTerminate.TradeTerminate;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_STEPS)
public class TradeSteps {
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_execution_id", referencedColumnName = "id")
    private TradeExecution tradeExecution;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_monitoring_id", referencedColumnName = "id")
    private TradeMonitor tradeMonitoring;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "trade_termination_id", referencedColumnName = "id")
    private TradeTerminate tradeTermination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

}
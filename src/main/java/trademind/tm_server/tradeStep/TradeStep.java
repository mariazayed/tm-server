package trademind.tm_server.tradeStep;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.TradeUpdateEnum;
import trademind.tm_server.trade.Trade;

import java.time.LocalDateTime;

// Event log for what happened *during* a trade -- SL moved to break-even,
// trailed, scaled in/out, etc. A single Trade row can't capture a sequence of
// events, and the sequence itself is useful analysis material. Auto-populated
// by diffing MT5's order-modification history per position (Phase 4+); manual
// trades can log steps too.
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_STEPS)
public class TradeStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeUpdateEnum type;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    private Double priceAtEvent;
}

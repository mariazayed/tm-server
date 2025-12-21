package trademind.tm_server.tradeExecution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;
import trademind.tm_server.trade.TradeSteps;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_EXECUTIONS)
public class TradeExecution extends BaseEntity {
    /// ********** Timing & Session **********
    @Column(nullable = false)
    private LocalDateTime enteredAt;

    @Column(nullable = false)
    private SessionEnum session;

    /// ********** Core Trade Details **********
    @Column(nullable = false)
    private TradeDirectionEnum direction;

    @Column(nullable = false)
    private double lotSize;

    /// ********** Execution Logic **********
    @Column(nullable = false)
    private List<EntryReasonEnum> entryReasons;

    @Column(nullable = false)
    private List<EntryConfirmationEnum> entryConfirmations;

    @Column(nullable = false)
    private EntryTypeEnum entryType;

    /// ********** Classification **********
    @Column(nullable = false)
    private List<TradeTagEnum> tradeTags;

    @OneToOne(mappedBy = "tradeExecution")
    private TradeSteps tradeSteps;
}
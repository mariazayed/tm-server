package trademind.tm_server.tradeExecution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.*;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_TRADE_PLANS)
public class TradeExecution extends BaseEntity {
    private LocalDateTime enteredAt;

    @Column(nullable = false)
    private TradeDirectionEnum direction;

    @Column(nullable = false)
    private List<EntryReasonEnum> entryReasons;

    @Column(nullable = false)
    private List<EntryConfirmationEnum> entryConfirmations;

    @Column(nullable = false)
    private double lotSize;

    @Column(nullable = false)
    private EntryTypeEnum entryType;

    @Column(nullable = false)
    private List<TradeTagEnum> tradeTags;

    @Column(nullable = false)
    private SessionEnum session;
}
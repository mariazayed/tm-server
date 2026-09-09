package trademind.tm_server.capital;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.CapitalReasonEnum;
import trademind.tm_server.enums.TradeDirectionEnum;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_CAPITALS)
public class Capital extends BaseEntity {
    private LocalDateTime dateTime;
    private double amount;
    @Enumerated(EnumType.STRING)
    private CapitalReasonEnum reason;
}
package trademind.tm_server.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.Trade;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_ASSETS)
@Schema(description = "Entity representing a trading asset (cryptocurrency or token) that can be traded in the system")
public class Asset extends BaseEntity {

    @Schema(
            description = "Name of the cryptocurrency or token",
            example = "Bitcoin",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Column(nullable = false)
    private String name;

    @Schema(
            description = "Trading symbol or ticker used on exchanges. Typically includes the quote currency (e.g., USDT, USD, BTC)",
            example = "BTCUSDT",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Column(nullable = false)
    private String symbol;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;

}
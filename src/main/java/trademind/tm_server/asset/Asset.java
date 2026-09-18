package trademind.tm_server.asset;

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
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_ASSETS)
public class Asset extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;

    // Units per 1.0 lot -- e.g. 100000 for a standard forex lot, 1 for a
    // direct share, 100 for a typical options contract. Powers
    // TradeEnrichmentService's risk-in-currency calculations; left null
    // until set, in which case risk fields are simply skipped rather than
    // guessed. Only accurate when the account's currency matches the
    // asset's quote currency (e.g. a USD account trading XXX/USD pairs) --
    // a cross-currency account would need a conversion step this doesn't
    // attempt.
    private Double contractSize;

    // Price distance that equals 1 pip for this asset -- e.g. 0.0001 for a
    // 4-decimal forex pair, 0.01 for a 2-decimal JPY pair, 0.1 for XAUUSD.
    // Powers the pip figure on manually-entered journal trades (the MT5
    // importer derives this itself from the broker's symbol digits, so it
    // doesn't need this field). Left null until set, in which case pips are
    // simply skipped for trades on this asset rather than guessed.
    private Double pipSize;
}

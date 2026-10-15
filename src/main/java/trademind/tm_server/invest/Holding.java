package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;

import java.math.BigDecimal;

/** A fund or stock you hold and buy. Every buy belongs to exactly one. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_HOLDINGS)
public class Holding extends BaseEntity {

    /** Kinds of holding, stored by name. */
    public enum AssetClass { EQUITY, BONDS, CASH, REAL_ESTATE, COMMODITIES, OTHER }

    @Column(nullable = false, length = 16)
    private String ticker;

    @Column(length = 120)
    private String name;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AssetClass assetClass = AssetClass.EQUITY;

    @Column(nullable = false, length = 32)
    private String region = "Global";

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPct = new BigDecimal("100");

    @Column(precision = 5, scale = 3)
    private BigDecimal expenseRatio;
}

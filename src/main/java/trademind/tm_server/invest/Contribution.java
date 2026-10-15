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
import java.time.LocalDate;

/** One monthly buy. Cash amounts are cents; shares and price are exact decimals. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_CONTRIBUTIONS)
public class Contribution extends BaseEntity {

    @Column(nullable = false)
    private LocalDate contributedOn;

    @Column(nullable = false)
    private long amountCents;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal shares;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private long feeCents;

    /** The fund this buy was for. */
    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @jakarta.persistence.JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;
}

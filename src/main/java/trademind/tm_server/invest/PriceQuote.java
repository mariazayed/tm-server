package trademind.tm_server.invest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trademind.tm_server.constants.AppConstants;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cached daily close for a ticker; unique per (ticker, date). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = AppConstants.DB_INVEST_PRICES)
public class PriceQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String ticker;

    @Column(nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal close;

    public PriceQuote(String ticker, LocalDate priceDate, BigDecimal close) {
        this.ticker = ticker;
        this.priceDate = priceDate;
        this.close = close;
    }
}

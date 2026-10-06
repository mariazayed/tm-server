package trademind.tm_server.invest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.invest.PriceQuote;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, Long> {
    Optional<PriceQuote> findByTickerAndPriceDate(String ticker, LocalDate priceDate);

    Optional<PriceQuote> findFirstByTickerOrderByPriceDateDesc(String ticker);

    List<PriceQuote> findByTickerAndPriceDateBetweenOrderByPriceDateAsc(
            String ticker, LocalDate from, LocalDate to);
}

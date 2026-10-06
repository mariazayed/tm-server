package trademind.tm_server.invest.price;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Source of end-of-day prices. Swap the implementation without touching the rest of the app. */
public interface PriceProvider {

    record DailyClose(LocalDate date, BigDecimal close) {
    }

    /** Daily closes for [from, to] inclusive, oldest first. Non-trading days are simply absent. */
    List<DailyClose> fetchDailyCloses(String ticker, LocalDate from, LocalDate to);

    /** Thrown for any provider failure: network, rate limit, bad payload. */
    class PriceProviderException extends RuntimeException {
        public PriceProviderException(String message) {
            super(message);
        }

        public PriceProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

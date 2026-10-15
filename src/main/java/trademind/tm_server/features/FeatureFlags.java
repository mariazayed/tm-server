package trademind.tm_server.features;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for which product areas are switched on. Everything that needs to know
 * (the {@code /features} endpoint now, the trading route gate later) asks this bean and never
 * reads the property itself.
 *
 * <p>Trading is off by default: this is an investing-first app. Data is never touched by the
 * flag, so turning it back on restores everything.
 */
@Component
public class FeatureFlags {

    private final boolean trading;

    public FeatureFlags(@Value("${features.trading.enabled:false}") boolean trading) {
        this.trading = trading;
    }

    public boolean trading() {
        return trading;
    }

    /** Investing is the product itself and cannot be switched off. */
    public boolean investing() {
        return true;
    }
}

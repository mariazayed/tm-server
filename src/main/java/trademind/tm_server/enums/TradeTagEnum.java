package trademind.tm_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TradeTagEnum {
    TREND_TRADE(1),
    REVERSAL_TRADE(2),
    LIQUIDITY_SWEEP_TRADE(3),
    BREAKOUT_TRADE(4),
    FAILED_BREAKOUT_TRADE(5),
    NEWS_TRADE(6),
    SMC_TRADE(7);

    private final int index;

    TradeTagEnum(int index) {
        this.index = index;
    }

    @JsonValue
    public int getValue() {
        return ordinal();
    }
}

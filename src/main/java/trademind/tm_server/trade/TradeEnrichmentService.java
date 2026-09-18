package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.asset.Asset;
import trademind.tm_server.asset.AssetService;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.DayOfWeekEnum;
import trademind.tm_server.enums.SessionEnum;
import trademind.tm_server.enums.TradeTypeEnum;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;

// Computes deterministic server-owned fields (session, dayOfWeek, tradeType,
// riskPercent, plannedRiskReward, rMultiple). Pips are intentionally owned by
// the frontend calculator, which reads the selected Asset row and sends the
// result on create.
//
// accountBalanceAtEntry and MAE/MFE are deliberately NOT computed here -- they
// need MT5 tick/balance history this backend doesn't have, so the importer
// supplies those as-is.
//
// The pure calculations below are package-private statics so they can be unit
// tested directly (see TradeEnrichmentServiceTest) without a Spring context.
@Service
public class TradeEnrichmentService {
    @Autowired
    private AssetService assetService;

    public void enrich(Trade trade) {
        applyDayOfWeekAndSession(trade);
        applyTradeType(trade);
        applyRiskMetrics(trade);
    }

    private void applyRiskMetrics(Trade trade) {
        if (trade.getAsset() == null) {
            return;
        }

        Asset asset;
        try {
            asset = assetService.getOneByIdAsEntity(trade.getAsset().getId());
        } catch (ResourceNotFoundException e) {
            // Shouldn't happen -- asset_id is a NOT NULL FK -- but enrichment
            // failing shouldn't take the whole trade-create request down with it.
            return;
        }

        Double contractSize = asset.getContractSize();
        if (contractSize == null || contractSize <= 0) {
            // Asset not configured for risk calc yet -- leave the fields null
            // rather than guess.
            return;
        }

        double riskAmount = riskAmountFor(trade.getEntryPrice(), trade.getSlPrice(), trade.getLotSize(), contractSize);
        if (riskAmount <= 0) {
            return;
        }

        if (trade.getAccountBalanceAtEntry() != null && trade.getAccountBalanceAtEntry() > 0) {
            trade.setRiskPercent(riskPercentFor(riskAmount, trade.getAccountBalanceAtEntry()));
        }

        Double riskReward = riskRewardFor(trade.getEntryPrice(), trade.getSlPrice(), trade.getTpPrice());
        if (riskReward != null) {
            trade.setPlannedRiskReward(riskReward);
        }

        trade.setRMultiple(trade.getPnl() / riskAmount);
    }

    private void applyDayOfWeekAndSession(Trade trade) {
        if (trade.getEntryDateTime() == null) {
            return;
        }
        trade.setDayOfWeek(dayOfWeekFor(trade.getEntryDateTime()));
        trade.setSession(sessionFor(trade.getEntryDateTime()));
    }

    private void applyTradeType(Trade trade) {
        if (trade.getEntryDateTime() == null || trade.getExitDateTime() == null) {
            return;
        }
        TradeTypeEnum tradeType = tradeTypeFor(trade.getEntryDateTime(), trade.getExitDateTime());
        if (tradeType != null) {
            trade.setTradeType(tradeType);
        }
    }

    static DayOfWeekEnum dayOfWeekFor(LocalDateTime entryDateTime) {
        return DayOfWeekEnum.valueOf(entryDateTime.getDayOfWeek().name());
    }

    // Non-overlapping UTC-hour buckets for a single-value classification.
    // Real trading sessions overlap; this picks one label per trade rather
    // than modeling the overlap.
    static SessionEnum sessionFor(LocalDateTime entryDateTimeUtc) {
        int hour = entryDateTimeUtc.getHour();
        if (hour >= AppConstants.LONDON_SESSION_START_HOUR_UTC && hour < AppConstants.NEW_YORK_SESSION_START_HOUR_UTC) {
            return SessionEnum.LONDON_SESSION;
        }
        if (hour >= AppConstants.NEW_YORK_SESSION_START_HOUR_UTC) {
            return SessionEnum.NEW_YORK_SESSION;
        }
        return SessionEnum.ASIAN_SESSION;
    }

    // Returns null for bad data (exit before entry) rather than guessing.
    static TradeTypeEnum tradeTypeFor(LocalDateTime entryDateTime, LocalDateTime exitDateTime) {
        Duration duration = Duration.between(entryDateTime, exitDateTime);
        if (duration.isNegative()) {
            return null;
        }
        long minutes = duration.toMinutes();
        if (minutes < AppConstants.SCALPING_MAX_DURATION_MINUTES) {
            return TradeTypeEnum.SCALPING;
        }
        if (minutes < AppConstants.INTRADAY_MAX_DURATION_MINUTES) {
            return TradeTypeEnum.INTRADAY;
        }
        return TradeTypeEnum.SWING;
    }

    // Notional risk in account-currency terms. Only correct when the account's
    // currency matches the asset's quote currency (see Asset.contractSize).
    static double riskAmountFor(double entryPrice, double slPrice, double lotSize, double contractSize) {
        return Math.abs(entryPrice - slPrice) * lotSize * contractSize;
    }

    static double riskPercentFor(double riskAmount, double accountBalance) {
        return riskAmount / accountBalance * 100;
    }

    // Returns null when the stop distance is zero (no risk to compare against)
    // instead of dividing by zero.
    static Double riskRewardFor(double entryPrice, double slPrice, double tpPrice) {
        double stopDistance = Math.abs(entryPrice - slPrice);
        if (stopDistance == 0) {
            return null;
        }
        double rewardDistance = Math.abs(tpPrice - entryPrice);
        return rewardDistance / stopDistance;
    }
}

package trademind.tm_server.capital.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import trademind.tm_server.base.BaseSearchRequest;
import trademind.tm_server.enums.CapitalReasonEnum;

import java.util.List;

/**
 * Search/pagination request for POST /api/v1/capital/search. A Capital row
 * is booked for every deposit/withdraw AND for every trade's PROFIT/LOSS,
 * so this table grows with trade volume too -- same lazy-loading motivation
 * as TradeSearchRequest.
 *
 * [reasons] lets a caller restrict to a small set of reasons -- e.g. the
 * Deposits & Withdraws screen passes [DEPOSIT, WITHDRAW] so pagination
 * doesn't have to wade through every PROFIT/LOSS row to find the handful of
 * actual deposits/withdrawals.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CapitalSearchRequest extends BaseSearchRequest {
    private List<CapitalReasonEnum> reasons;
}

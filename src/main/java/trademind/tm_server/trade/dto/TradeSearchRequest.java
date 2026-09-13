package trademind.tm_server.trade.dto;

import trademind.tm_server.base.BaseSearchRequest;

/**
 * Search/pagination request for POST /api/v1/trade/search.
 *
 * No extra filter fields yet -- this exists to give the Trade List, Trade
 * Journal and Trade Review screens real server-side pagination
 * (page/size/sortBy/direction, inherited from BaseSearchRequest) instead of
 * TradeService.getAll() returning the entire, ever-growing (auto-imported)
 * trade history on every screen visit. Client-side filters (asset,
 * strategy, direction, result, review status, search text, date range)
 * still apply on whatever pages have been loaded so far -- adding true
 * server-side filtering for all of those would need range/OR predicates
 * BaseSpecification doesn't support, so it's left for a follow-up.
 */
public class TradeSearchRequest extends BaseSearchRequest {
}

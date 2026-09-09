-- Phase 2: a trade shouldn't carry the exact same mistake tag twice. The
-- entity now maps this as a Set<MistakeTagEnum> (dedupes in the JVM), but a
-- DB-level constraint is cheap insurance against concurrent writes bypassing
-- that.
ALTER TABLE trade_mistake_tags
    ADD CONSTRAINT ux_trade_mistake_tags UNIQUE (trade_id, mistake_tag);

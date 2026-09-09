-- Phase 0: several enum-typed columns had no @Enumerated annotation, so Hibernate
-- defaulted to ORDINAL storage (the enum's declaration-order integer, via
-- Enum.ordinal()) instead of the readable name. Those entities now declare
-- @Enumerated(EnumType.STRING); this migration converts the already-stored
-- integers to match, using each enum's *declaration order* below -- NOT the
-- custom numeric `index` field those enums also carry for JSON serialization,
-- which is unrelated and must not be used for this mapping.
--
-- Every block is guarded (checks information_schema first) so this migration is
-- a safe no-op against a table/column that doesn't exist yet, or that isn't
-- still stored as an integer -- e.g. `goals` may not exist yet in every
-- environment since that entity is newer than the others.
--
-- Each block also drops any pre-existing CHECK constraint on the column before
-- retyping it. Some of these ordinal columns picked up a numeric range check
-- (e.g. `direction >= 0 AND direction <= 1`) back when they were plain
-- integers. Postgres re-validates CHECK constraints against the new value as
-- part of the ALTER COLUMN ... TYPE rewrite, and a numeric check evaluated
-- against the new varchar value fails with "operator does not exist:
-- character varying >= integer". The DROP is looked up dynamically (by
-- column, not by a guessed constraint name) so this is safe to run even if
-- no such constraint exists in a given environment.

-- trades.direction  (TradeDirectionEnum: 0=LONG, 1=SHORT)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'trades' AND column_name = 'direction' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'trades'::regclass
              AND con.contype = 'c'
              AND att.attname = 'direction'
        LOOP
            EXECUTE format('ALTER TABLE trades DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE trades ALTER COLUMN direction TYPE varchar(255) USING (
            CASE direction
                WHEN 0 THEN 'LONG'
                WHEN 1 THEN 'SHORT'
            END
        );
    END IF;
END $$;

-- days.day_of_week  (DayOfWeekEnum: 0=MONDAY, 1=TUESDAY, 2=WEDNESDAY, 3=THURSDAY, 4=FRIDAY, 5=SATURDAY, 6=SUNDAY)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'days' AND column_name = 'day_of_week' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'days'::regclass
              AND con.contype = 'c'
              AND att.attname = 'day_of_week'
        LOOP
            EXECUTE format('ALTER TABLE days DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE days ALTER COLUMN day_of_week TYPE varchar(255) USING (
            CASE day_of_week
                WHEN 0 THEN 'MONDAY'
                WHEN 1 THEN 'TUESDAY'
                WHEN 2 THEN 'WEDNESDAY'
                WHEN 3 THEN 'THURSDAY'
                WHEN 4 THEN 'FRIDAY'
                WHEN 5 THEN 'SATURDAY'
                WHEN 6 THEN 'SUNDAY'
            END
        );
    END IF;
END $$;

-- days.psychology_rate  (ScoreEnum: 0=TERRIBLE .. 9=WORLD_CLASS; nullable, CASE preserves NULLs)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'days' AND column_name = 'psychology_rate' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'days'::regclass
              AND con.contype = 'c'
              AND att.attname = 'psychology_rate'
        LOOP
            EXECUTE format('ALTER TABLE days DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE days ALTER COLUMN psychology_rate TYPE varchar(255) USING (
            CASE psychology_rate
                WHEN 0 THEN 'TERRIBLE'
                WHEN 1 THEN 'VERY_POOR'
                WHEN 2 THEN 'POOR'
                WHEN 3 THEN 'BELOW_AVERAGE'
                WHEN 4 THEN 'AVERAGE'
                WHEN 5 THEN 'ABOVE_AVERAGE'
                WHEN 6 THEN 'GOOD'
                WHEN 7 THEN 'VERY_GOOD'
                WHEN 8 THEN 'EXCELLENT'
                WHEN 9 THEN 'WORLD_CLASS'
            END
        );
    END IF;
END $$;

-- capitals.reason  (CapitalReasonEnum: 0=PROFIT, 1=LOSS, 2=WITHDRAW, 3=DEPOSIT)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'capitals' AND column_name = 'reason' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'capitals'::regclass
              AND con.contype = 'c'
              AND att.attname = 'reason'
        LOOP
            EXECUTE format('ALTER TABLE capitals DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE capitals ALTER COLUMN reason TYPE varchar(255) USING (
            CASE reason
                WHEN 0 THEN 'PROFIT'
                WHEN 1 THEN 'LOSS'
                WHEN 2 THEN 'WITHDRAW'
                WHEN 3 THEN 'DEPOSIT'
            END
        );
    END IF;
END $$;

-- goals.category  (GoalCategoryEnum: 0=PROFIT, 1=TRADE_QUALITY, 2=DISCIPLINE, 3=MANUAL)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'goals' AND column_name = 'category' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'goals'::regclass
              AND con.contype = 'c'
              AND att.attname = 'category'
        LOOP
            EXECUTE format('ALTER TABLE goals DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE goals ALTER COLUMN category TYPE varchar(255) USING (
            CASE category
                WHEN 0 THEN 'PROFIT'
                WHEN 1 THEN 'TRADE_QUALITY'
                WHEN 2 THEN 'DISCIPLINE'
                WHEN 3 THEN 'MANUAL'
            END
        );
    END IF;
END $$;

-- goals.status  (GoalStatusEnum: 0=ACTIVE, 1=ACHIEVED, 2=FAILED, 3=ARCHIVED)
DO $$
DECLARE
    conname text;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'goals' AND column_name = 'status' AND data_type IN ('integer', 'smallint')
    ) THEN
        FOR conname IN
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_attribute att
                ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
            WHERE con.conrelid = 'goals'::regclass
              AND con.contype = 'c'
              AND att.attname = 'status'
        LOOP
            EXECUTE format('ALTER TABLE goals DROP CONSTRAINT %I', conname);
        END LOOP;

        ALTER TABLE goals ALTER COLUMN status TYPE varchar(255) USING (
            CASE status
                WHEN 0 THEN 'ACTIVE'
                WHEN 1 THEN 'ACHIEVED'
                WHEN 2 THEN 'FAILED'
                WHEN 3 THEN 'ARCHIVED'
            END
        );
    END IF;
END $$;

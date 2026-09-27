-- Planner follow-up:
-- 1) Extra working hours become separate entries per week, each optionally
--    tagged with a Focus Area ("+6h Backtesting, weekend session"). An
--    entry tagged with an area adds straight to that area's allowance; an
--    untagged ("general") entry raises total capacity, so every area's
--    max % scales with it. Existing single per-week values are migrated as
--    general entries.
-- 2) Task / objective titles become free text (edited in a text area).

CREATE TABLE planner_week_extras (
    id             bigserial        PRIMARY KEY,
    week_start     date             NOT NULL,
    hours          double precision NOT NULL,
    focus_area_id  bigint           REFERENCES focus_areas(id),
    created_at     timestamp        NOT NULL DEFAULT now(),
    updated_at     timestamp,
    deleted_at     timestamp,
    notes          text
);

CREATE INDEX idx_planner_week_extras_week_start ON planner_week_extras(week_start);

COMMENT ON COLUMN planner_week_extras.week_start IS 'Monday of the week the extra hours belong to';
COMMENT ON COLUMN planner_week_extras.focus_area_id IS 'Null = general extra time (scales every area by its max %); set = hours dedicated to that Focus Area';
COMMENT ON COLUMN planner_week_extras.notes IS 'Why the extra hours (e.g. weekend backtesting session)';

INSERT INTO planner_week_extras (week_start, hours, notes, created_at)
SELECT week_start, extra_hours, extra_hours_note, now()
FROM planner_weeks
WHERE extra_hours > 0 AND deleted_at IS NULL;

ALTER TABLE planner_weeks DROP COLUMN extra_hours;
ALTER TABLE planner_weeks DROP COLUMN extra_hours_note;

ALTER TABLE weekly_tasks ALTER COLUMN title TYPE text;
ALTER TABLE monthly_objectives ALTER COLUMN title TYPE text;

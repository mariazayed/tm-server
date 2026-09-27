-- Planner feature: Focus Areas, weekly/monthly capacity, weekly tasks and
-- monthly objectives with evaluation. Shaped to match the entities in
-- trademind.tm_server.planner (BaseEntity fields + each entity's own), same
-- column-naming convention as goals/decisions/expenses (see V8/V10/V14).

-- Focus Areas: user-defined buckets of work (Backtesting, Journaling,
-- Learning...). max_percent caps the share of a week's / month's capacity
-- that tasks in this area should take up (soft limit: warn, never block).
CREATE TABLE focus_areas (
    id           bigserial        PRIMARY KEY,
    name         varchar(255)     NOT NULL,
    color        varchar(255)     NOT NULL,
    icon         varchar(255),
    max_percent  double precision NOT NULL,
    sort_order   integer          NOT NULL DEFAULT 0,
    archived     boolean          NOT NULL DEFAULT false,
    created_at   timestamp        NOT NULL DEFAULT now(),
    updated_at   timestamp,
    deleted_at   timestamp,
    notes        text
);

COMMENT ON COLUMN focus_areas.max_percent IS 'Max share (0-100) of weekly/monthly capacity this area may take -- soft limit, UI warns when exceeded';
COMMENT ON COLUMN focus_areas.archived IS 'Archived areas are hidden from pickers but kept for historical tasks';

-- Single-row settings table: working hours per weekday. Weekly capacity is
-- the sum of the seven columns; working days are the days with hours > 0;
-- monthly capacity walks the calendar days of the month.
CREATE TABLE planner_settings (
    id               bigserial        PRIMARY KEY,
    monday_hours     double precision NOT NULL DEFAULT 0,
    tuesday_hours    double precision NOT NULL DEFAULT 0,
    wednesday_hours  double precision NOT NULL DEFAULT 0,
    thursday_hours   double precision NOT NULL DEFAULT 0,
    friday_hours     double precision NOT NULL DEFAULT 0,
    saturday_hours   double precision NOT NULL DEFAULT 0,
    sunday_hours     double precision NOT NULL DEFAULT 0,
    created_at       timestamp        NOT NULL DEFAULT now(),
    updated_at       timestamp,
    deleted_at       timestamp,
    notes            text
);

INSERT INTO planner_settings (monday_hours, tuesday_hours, wednesday_hours, thursday_hours, friday_hours, saturday_hours, sunday_hours)
VALUES (6, 6, 6, 6, 6, 0, 0);

-- One row per planned week (Monday start) -- only created once the user
-- adds extra hours or a week note, so a missing row means "no extras".
CREATE TABLE planner_weeks (
    id                bigserial        PRIMARY KEY,
    week_start        date             NOT NULL UNIQUE,
    extra_hours       double precision NOT NULL DEFAULT 0,
    extra_hours_note  varchar(255),
    created_at        timestamp        NOT NULL DEFAULT now(),
    updated_at        timestamp,
    deleted_at        timestamp,
    notes             text
);

COMMENT ON COLUMN planner_weeks.week_start IS 'Monday of the planned week';
COMMENT ON COLUMN planner_weeks.extra_hours IS 'Hours on top of the weekday settings for this week only; counted in the month the week starts in';

CREATE TABLE monthly_objectives (
    id                bigserial        PRIMARY KEY,
    plan_year         integer          NOT NULL,
    plan_month        integer          NOT NULL,
    title             varchar(255)     NOT NULL,
    focus_area_id     bigint           NOT NULL REFERENCES focus_areas(id),
    estimated_hours   double precision NOT NULL DEFAULT 0,
    status            varchar(255)     NOT NULL,
    score             integer,
    actual_hours      double precision,
    evaluation_notes  text,
    sort_order        integer          NOT NULL DEFAULT 0,
    created_at        timestamp        NOT NULL DEFAULT now(),
    updated_at        timestamp,
    deleted_at        timestamp,
    notes             text
);

CREATE INDEX idx_monthly_objectives_period ON monthly_objectives(plan_year, plan_month);
CREATE INDEX idx_monthly_objectives_focus_area_id ON monthly_objectives(focus_area_id);

COMMENT ON COLUMN monthly_objectives.plan_month IS '1-12';
COMMENT ON COLUMN monthly_objectives.status IS 'PlanItemStatusEnum: PLANNED, DONE, PARTIAL, MISSED';
COMMENT ON COLUMN monthly_objectives.score IS 'Evaluation score 1-5, null until evaluated';

CREATE TABLE weekly_tasks (
    id                    bigserial        PRIMARY KEY,
    week_start            date             NOT NULL,
    title                 varchar(255)     NOT NULL,
    focus_area_id         bigint           NOT NULL REFERENCES focus_areas(id),
    monthly_objective_id  bigint           REFERENCES monthly_objectives(id),
    estimated_hours       double precision NOT NULL DEFAULT 0,
    status                varchar(255)     NOT NULL,
    score                 integer,
    actual_hours          double precision,
    evaluation_notes      text,
    sort_order            integer          NOT NULL DEFAULT 0,
    created_at            timestamp        NOT NULL DEFAULT now(),
    updated_at            timestamp,
    deleted_at            timestamp,
    notes                 text
);

CREATE INDEX idx_weekly_tasks_week_start ON weekly_tasks(week_start);
CREATE INDEX idx_weekly_tasks_focus_area_id ON weekly_tasks(focus_area_id);
CREATE INDEX idx_weekly_tasks_monthly_objective_id ON weekly_tasks(monthly_objective_id);

COMMENT ON COLUMN weekly_tasks.week_start IS 'Monday of the week this task is planned for';
COMMENT ON COLUMN weekly_tasks.monthly_objective_id IS 'Optional link -- the objective''s progress rolls up from its linked tasks';
COMMENT ON COLUMN weekly_tasks.status IS 'PlanItemStatusEnum: PLANNED, DONE, PARTIAL, MISSED';

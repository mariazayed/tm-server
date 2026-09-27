-- Extra hours become weekly tasks flagged `extra`: they sit in the task list
-- under their Focus Area, count as planned work, can be evaluated and have
-- time logged -- and each one also raises the week's capacity and its
-- area's budget by its estimated hours.
--
-- Existing planner_week_extras rows are moved over. Rows created without a
-- Focus Area go to the first active one (by sort order); they are
-- soft-deleted from planner_week_extras once copied. The old table is left
-- in place (no longer read) so nothing is lost.

ALTER TABLE weekly_tasks ADD COLUMN extra boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN weekly_tasks.extra IS 'True = extra working hours: counts as planned work AND adds its estimated hours to the week capacity and to its Focus Area budget';

INSERT INTO weekly_tasks (week_start, title, focus_area_id, estimated_hours, status, sort_order, created_at, notes, extra)
SELECT e.week_start,
       COALESCE(NULLIF(btrim(split_part(e.notes, E'\n', 1)), ''), 'Extra hours'),
       COALESCE(e.focus_area_id,
                (SELECT f.id FROM focus_areas f WHERE f.deleted_at IS NULL ORDER BY f.archived, f.sort_order, f.id LIMIT 1)),
       e.hours,
       'PLANNED',
       1000,
       now(),
       e.notes,
       true
FROM planner_week_extras e
WHERE e.deleted_at IS NULL
  AND COALESCE(e.focus_area_id,
               (SELECT f.id FROM focus_areas f WHERE f.deleted_at IS NULL ORDER BY f.archived, f.sort_order, f.id LIMIT 1)) IS NOT NULL;

UPDATE planner_week_extras
SET deleted_at = now()
WHERE deleted_at IS NULL
  AND (focus_area_id IS NOT NULL OR EXISTS (SELECT 1 FROM focus_areas f WHERE f.deleted_at IS NULL));

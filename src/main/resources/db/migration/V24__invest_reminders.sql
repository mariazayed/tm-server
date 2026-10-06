-- Buy-day reminders (Phase 7): per-plan preferences on the settings row, plus a log that makes
-- sure each reminder is sent once per month even if the server restarts or the job re-runs.

ALTER TABLE invest_settings
    ADD COLUMN reminders_enabled       boolean     NOT NULL DEFAULT false,
    ADD COLUMN reminder_email          varchar(254),
    ADD COLUMN reminder_followup_days  smallint    NOT NULL DEFAULT 3
        CONSTRAINT ck_invest_settings_followup CHECK (reminder_followup_days BETWEEN 1 AND 14);

COMMENT ON COLUMN invest_settings.reminder_followup_days IS 'Days after the buy day to nudge again if nothing was bought';

CREATE TABLE invest_reminder_log (
    id      bigserial   PRIMARY KEY,
    month   varchar(7)  NOT NULL,
    kind    varchar(16) NOT NULL,
    sent_at timestamp   NOT NULL DEFAULT now(),
    CONSTRAINT uq_invest_reminder_log UNIQUE (month, kind)
);

COMMENT ON COLUMN invest_reminder_log.month IS 'YYYY-MM the reminder was for';
COMMENT ON COLUMN invest_reminder_log.kind IS 'ReminderKind: DUE, FOLLOWUP';

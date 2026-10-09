-- How long the owner's own rule says to wait between deciding to deviate from the plan and doing it
-- (AI assistant, step 6). The rule text itself is free text and is never parsed; this number is what the
-- wait-period check compares against. 0 turns the check off.

ALTER TABLE invest_settings
    ADD COLUMN rule_wait_hours integer NOT NULL DEFAULT 72
    CHECK (rule_wait_hours BETWEEN 0 AND 720);

COMMENT ON COLUMN invest_settings.rule_wait_hours IS 'Hours to wait before a deviation; 0 disables the wait-period check';

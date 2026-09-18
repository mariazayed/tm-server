-- Expenses feature (personal expense log, standalone from Capital/dashboard).
-- Two tables: expense_reasons is a user-managed lookup (add/rename/delete
-- from the UI, not a fixed Java enum like CapitalReasonEnum), and expenses
-- references it by FK. Shaped to match ExpenseReason.java/Expense.java
-- (BaseEntity fields + each entity's own), same column-naming convention as
-- goals/decisions (see V8/V10).

CREATE TABLE expense_reasons (
    id          bigserial    PRIMARY KEY,
    name        varchar(255) NOT NULL,
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp,
    deleted_at  timestamp,
    notes       text
);

CREATE TABLE expenses (
    id                 bigserial        PRIMARY KEY,
    date               timestamp        NOT NULL,
    amount             double precision NOT NULL,
    expense_reason_id  bigint           NOT NULL REFERENCES expense_reasons(id),
    created_at         timestamp        NOT NULL DEFAULT now(),
    updated_at         timestamp,
    deleted_at         timestamp,
    notes              text
);

CREATE INDEX idx_expenses_expense_reason_id ON expenses(expense_reason_id);

COMMENT ON COLUMN expenses.amount IS 'USD amount spent';
COMMENT ON COLUMN expenses.expense_reason_id IS 'FK to expense_reasons -- soft-deleted reasons are kept for historical expenses, just hidden from the picker for new ones';

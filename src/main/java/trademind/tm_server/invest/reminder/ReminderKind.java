package trademind.tm_server.invest.reminder;

public enum ReminderKind {
    /** On (or after) the buy day. */
    DUE,
    /** A few days later, when nothing has been bought yet. */
    FOLLOWUP
}

package trademind.tm_server.invest.reminder;

/** Delivery channel for reminders (email today; push or others could implement this too). */
public interface ReminderSender {

    /** True when this channel can actually deliver (e.g. SMTP is configured). */
    boolean isConfigured();

    /** @throws ReminderException when the message could not be sent */
    void send(String to, String subject, String body);

    class ReminderException extends RuntimeException {
        public ReminderException(String message) {
            super(message);
        }

        public ReminderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

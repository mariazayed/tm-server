package trademind.tm_server.invest.reminder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Plain-text email through Spring's JavaMailSender; exists only when SMTP is configured. */
@Component
public class EmailReminderSender implements ReminderSender {

    private final ObjectProvider<JavaMailSender> mail;
    private final ReminderProperties properties;

    public EmailReminderSender(ObjectProvider<JavaMailSender> mail, ReminderProperties properties) {
        this.mail = mail;
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return mail.getIfAvailable() != null && properties.from() != null && !properties.from().isBlank();
    }

    @Override
    public void send(String to, String subject, String body) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null || properties.from() == null || properties.from().isBlank()) {
            throw new ReminderException("Email is not configured on the server (spring.mail.host and invest.reminders.from)");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException e) {
            throw new ReminderException("Could not send the email: " + e.getMessage(), e);
        }
    }
}

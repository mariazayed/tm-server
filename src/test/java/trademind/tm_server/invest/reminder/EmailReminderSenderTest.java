package trademind.tm_server.invest.reminder;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailReminderSenderTest {

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    private static ReminderProperties props(String from) {
        return new ReminderProperties(true, "0 0 9 * * *", from);
    }

    @Test
    void sendsAPlainTextMessageFromTheConfiguredAddress() {
        JavaMailSender mail = mock(JavaMailSender.class);
        EmailReminderSender sender = new EmailReminderSender(provider(mail), props("tracker@example.com"));

        assertTrue(sender.isConfigured());
        sender.send("me@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(sent.capture());
        assertEquals("tracker@example.com", sent.getValue().getFrom());
        assertEquals("me@example.com", sent.getValue().getTo()[0]);
        assertEquals("Subject", sent.getValue().getSubject());
        assertEquals("Body", sent.getValue().getText());
    }

    @Test
    void reportsNotConfiguredWithoutSmtpOrSender() {
        assertFalse(new EmailReminderSender(provider(null), props("a@b.c")).isConfigured());
        assertFalse(new EmailReminderSender(provider(mock(JavaMailSender.class)), props(" ")).isConfigured());
        assertFalse(new EmailReminderSender(provider(mock(JavaMailSender.class)), props(null)).isConfigured());
    }

    @Test
    void refusesToSendWhenNotConfigured() {
        JavaMailSender mail = mock(JavaMailSender.class);
        EmailReminderSender sender = new EmailReminderSender(provider(mail), props(null));
        ReminderSender.ReminderException e =
                assertThrows(ReminderSender.ReminderException.class, () -> sender.send("me@example.com", "s", "b"));
        assertTrue(e.getMessage().contains("not configured"));
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void mailFailuresBecomeReminderExceptions() {
        JavaMailSender mail = mock(JavaMailSender.class);
        doThrow(new MailSendException("connection refused")).when(mail).send(any(SimpleMailMessage.class));
        EmailReminderSender sender = new EmailReminderSender(provider(mail), props("a@b.c"));
        ReminderSender.ReminderException e =
                assertThrows(ReminderSender.ReminderException.class, () -> sender.send("me@example.com", "s", "b"));
        assertTrue(e.getMessage().contains("connection refused"));
    }
}

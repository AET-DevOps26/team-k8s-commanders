package com.caredesk.notification.email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EmailSender}. The {@link JavaMailSender} is mocked so
 * the tests do not need a real SMTP server.
 */
class EmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailSender emailSender = new EmailSender(mailSender, "CareDesk <no-reply@caredesk.local>");

    @Test
    void send_dispatchesMessageWithFromToSubjectBody() {
        boolean ok = emailSender.send("anna@example.com", "Hello", "Body text");

        assertThat(ok).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("CareDesk <no-reply@caredesk.local>");
        assertThat(sent.getTo()).containsExactly("anna@example.com");
        assertThat(sent.getSubject()).isEqualTo("Hello");
        assertThat(sent.getText()).isEqualTo("Body text");
    }

    @Test
    void send_skipsWhenNoRecipient() {
        assertThat(emailSender.send(null, "Hello", "Body")).isFalse();
        assertThat(emailSender.send("  ", "Hello", "Body")).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_swallowsDeliveryFailure() {
        doThrow(new MailSendException("SMTP unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Must not throw — delivery is best-effort so booking flows are never blocked.
        boolean ok = emailSender.send("anna@example.com", "Hello", "Body");

        assertThat(ok).isFalse();
    }
}

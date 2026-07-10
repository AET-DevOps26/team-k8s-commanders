package com.caredesk.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends plain-text emails over SMTP, best-effort.
 *
 * <p>In dev, compose and the cluster the configured SMTP target is Mailpit,
 * which accepts every message and exposes it in a web UI — so a real mail
 * server is never required. Swapping in a production provider is purely an
 * {@code SMTP_*} configuration change.
 *
 * <p>Delivery is best-effort: a send failure (SMTP down, refused, timeout) is
 * logged and swallowed, returning {@code false}. Callers persist the
 * notification record regardless, so a missing mail server never blocks the
 * booking flow or loses the audit record.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    /**
     * @param mailSender the Spring mail sender, auto-configured from {@code spring.mail.*}
     * @param from       the {@code From} address for outgoing mail
     */
    public EmailSender(JavaMailSender mailSender,
                       @Value("${notification.mail.from:CareDesk <no-reply@caredesk.local>}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Attempts to send a plain-text email.
     *
     * @param to      recipient address; if null/blank the send is skipped
     * @param subject the email subject
     * @param body    the plain-text body
     * @return {@code true} if the message was handed to the SMTP server,
     *         {@code false} if it was skipped (no recipient) or delivery failed
     */
    public boolean send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email '{}' — no recipient address", subject);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent email '{}' to {}", subject, mask(to));
            return true;
        } catch (MailException e) {
            // Best-effort: never propagate. The notification record is still stored.
            log.error("Failed to send email '{}' to {}: {}", subject, mask(to), e.getMessage());
            return false;
        }
    }

    /**
     * Masks a recipient address for logging so application logs do not become a
     * secondary store of user contact data: keeps the domain and the first
     * local-part character, e.g. {@code a***@example.com}.
     *
     * @param address the recipient address
     * @return a masked form safe to log
     */
    private static String mask(String address) {
        int at = address.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return address.charAt(0) + "***" + address.substring(at);
    }
}

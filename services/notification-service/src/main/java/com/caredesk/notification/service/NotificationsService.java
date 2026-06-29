package com.caredesk.notification.service;

import com.caredesk.notification.email.EmailSender;
import com.caredesk.notification.model.Notification;
import com.caredesk.notification.model.NotificationType;
import com.caredesk.notification.repository.NotificationRepository;
import org.openapitools.model.NotificationChannel;
import org.openapitools.model.NotificationCreate;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedNotificationResponse;
import org.openapitools.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for notification records.
 *
 * <p>Reads are role-scoped: an {@code ADMIN} sees every notification (the
 * clinic admin monitors delivery), while a {@code PATIENT} only ever sees
 * notifications addressed to them. The caller's identity comes from the
 * trusted gateway headers and is passed in by the controllers.
 */
@Service
public class NotificationsService {

    /** Newest first — notification feeds read in reverse chronological order. */
    private static final Sort SENT_AT_DESC = Sort.by(Sort.Direction.DESC, "sentAt");

    private final NotificationRepository repository;
    private final EmailSender emailSender;

    /**
     * @param repository  Spring Data repository for {@link Notification}
     * @param emailSender best-effort SMTP sender for delivering notifications
     */
    public NotificationsService(NotificationRepository repository, EmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    /**
     * Records a notification and attempts to deliver it by email, in that order.
     *
     * <p>The record is persisted first, so a failed or skipped send never loses
     * the audit trail and never blocks the caller (booking triggers and the
     * reminder scheduler both rely on this). Delivery is best-effort — see
     * {@link EmailSender}.
     *
     * @param appointmentId  the appointment this notification refers to, may be {@code null}
     * @param patientId      the recipient patient's user id, may be {@code null}
     * @param recipientEmail the address to deliver to; if {@code null}/blank the
     *                       record is still stored but no email is sent
     * @param type           the internal classification (confirmation, reminder, …)
     * @param subject        the email subject line
     * @param message        the message body, also stored as the record's message
     * @return the persisted notification as an API DTO
     */
    @Transactional
    public org.openapitools.model.Notification recordAndSend(UUID appointmentId, UUID patientId,
                                                             String recipientEmail, NotificationType type,
                                                             String subject, String message) {
        Notification entity = new Notification();
        entity.setAppointmentId(appointmentId);
        entity.setPatientId(patientId);
        entity.setRecipientEmail(recipientEmail);
        entity.setType(type);
        entity.setChannel(NotificationChannel.EMAIL);
        entity.setMessage(message);
        entity.setSentAt(OffsetDateTime.now());
        org.openapitools.model.Notification saved = NotificationMapper.toModel(repository.save(entity));

        emailSender.send(recipientEmail, subject, message);
        return saved;
    }

    /**
     * Persists a new notification record. {@code sentAt} is stamped with the
     * current time — in this iteration a record represents the message as
     * delivered; actual email dispatch arrives in a later iteration.
     *
     * @param request the notification payload (message, channel, optional ids)
     * @return the persisted notification as an API DTO
     */
    @Transactional
    public org.openapitools.model.Notification create(NotificationCreate request) {
        Notification entity = new Notification();
        entity.setAppointmentId(request.getAppointmentId());
        entity.setPatientId(request.getPatientId());
        entity.setMessage(request.getMessage());
        entity.setChannel(request.getChannel());
        entity.setType(NotificationType.GENERIC);
        entity.setSentAt(OffsetDateTime.now());
        return NotificationMapper.toModel(repository.save(entity));
    }

    /**
     * Returns one page of notifications visible to the caller: all of them
     * for an admin, only their own for a patient.
     *
     * @param role   the caller's role from the gateway
     * @param userId the caller's user id from the gateway
     * @param page   zero-based page index
     * @param size   page size
     * @return the paginated API response
     */
    @Transactional(readOnly = true)
    public PaginatedNotificationResponse list(UserRole role, UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SENT_AT_DESC);
        Page<Notification> result = role == UserRole.ADMIN
                ? repository.findAll(pageable)
                : repository.findByPatientId(userId, pageable);
        return toPaginatedResponse(result);
    }

    /**
     * Returns a single notification by id. A patient may only read
     * notifications addressed to them.
     *
     * @param id     the notification id
     * @param role   the caller's role from the gateway
     * @param userId the caller's user id from the gateway
     * @return the notification as an API DTO
     * @throws ResponseStatusException 404 if no such notification exists
     * @throws AccessDeniedException   if a patient reads someone else's notification
     */
    @Transactional(readOnly = true)
    public org.openapitools.model.Notification getById(UUID id, UserRole role, UUID userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No notification exists with id " + id));
        if (role != UserRole.ADMIN && !userId.equals(notification.getPatientId())) {
            throw new AccessDeniedException("Not your notification");
        }
        return NotificationMapper.toModel(notification);
    }

    /**
     * Returns one page of notifications for an appointment. An admin sees
     * every notification on the appointment; a patient only the ones
     * addressed to them.
     *
     * @param appointmentId the appointment id
     * @param role          the caller's role from the gateway
     * @param userId        the caller's user id from the gateway
     * @param page          zero-based page index
     * @param size          page size
     * @return the paginated API response
     */
    @Transactional(readOnly = true)
    public PaginatedNotificationResponse listForAppointment(UUID appointmentId, UserRole role, UUID userId,
                                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SENT_AT_DESC);
        Page<Notification> result = role == UserRole.ADMIN
                ? repository.findByAppointmentId(appointmentId, pageable)
                : repository.findByAppointmentIdAndPatientId(appointmentId, userId, pageable);
        return toPaginatedResponse(result);
    }

    private static PaginatedNotificationResponse toPaginatedResponse(Page<Notification> result) {
        List<org.openapitools.model.Notification> content = result.getContent().stream()
                .map(NotificationMapper::toModel)
                .toList();
        PageMeta meta = new PageMeta()
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
        return new PaginatedNotificationResponse(content, meta);
    }
}

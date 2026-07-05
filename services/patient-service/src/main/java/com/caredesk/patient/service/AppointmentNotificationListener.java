package com.caredesk.patient.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispatches appointment notifications after the triggering transaction commits.
 *
 * <p>{@link AppointmentService} publishes an {@link AppointmentNotificationEvent}
 * inside its booking / reschedule / cancel transaction; this listener fires only
 * on {@link TransactionPhase#AFTER_COMMIT}, so a confirmation, reschedule or
 * cancellation email is never sent for a change that ultimately rolled back.
 * Running after commit also means the synchronous HTTP call no longer holds the
 * database transaction open.
 */
@Component
public class AppointmentNotificationListener {

    private final NotificationServiceClient notificationServiceClient;

    /**
     * @param notificationServiceClient best-effort client that forwards the
     *                                  trigger to notification-service
     */
    public AppointmentNotificationListener(NotificationServiceClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    /**
     * Forwards the trigger to notification-service once the appointment change
     * has committed. The client swallows any failure, so a notification problem
     * never surfaces to the caller.
     *
     * @param event the published notification trigger
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentNotification(AppointmentNotificationEvent event) {
        notificationServiceClient.notify(event.request());
    }
}

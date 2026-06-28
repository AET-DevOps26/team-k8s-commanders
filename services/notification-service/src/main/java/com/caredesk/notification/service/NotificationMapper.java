package com.caredesk.notification.service;

import com.caredesk.notification.model.Notification;

/**
 * Maps between the JPA entity ({@link Notification}) and the OpenAPI-generated
 * model of the same name in {@code org.openapitools.model}.
 *
 * <p>The two layers share a class name but live in different packages, so the
 * generated model is referenced by its fully-qualified name here to keep the
 * mapping unambiguous.
 */
public final class NotificationMapper {

    private NotificationMapper() {
    }

    /**
     * Converts a persisted notification into its API representation.
     *
     * @param entity the stored notification
     * @return the generated API model
     */
    public static org.openapitools.model.Notification toModel(Notification entity) {
        org.openapitools.model.Notification model = new org.openapitools.model.Notification(
                entity.getId(),
                entity.getMessage(),
                entity.getChannel(),
                entity.getSentAt());
        model.setAppointmentId(entity.getAppointmentId());
        model.setPatientId(entity.getPatientId());
        return model;
    }
}

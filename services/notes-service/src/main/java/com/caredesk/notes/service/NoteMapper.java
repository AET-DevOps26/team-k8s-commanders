package com.caredesk.notes.service;

import com.caredesk.notes.model.ClinicalNote;
import com.caredesk.notes.model.Diagnosis;

/**
 * Maps between the JPA entities ({@link ClinicalNote}, {@link Diagnosis}) and
 * the OpenAPI-generated models of the same names in {@code org.openapitools.model}.
 *
 * <p>The two layers share class names but live in different packages, so the
 * generated models are referenced by their fully-qualified names here to keep
 * the mapping unambiguous.
 */
public final class NoteMapper {

    private NoteMapper() {
    }

    /**
     * Converts a persisted note into its API representation.
     *
     * @param entity the stored note
     * @return the generated API model
     */
    public static org.openapitools.model.ClinicalNote toModel(ClinicalNote entity) {
        org.openapitools.model.ClinicalNote model = new org.openapitools.model.ClinicalNote(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getDoctorId(),
                entity.getContent(),
                entity.getCreatedAt());
        model.setDiagnosis(toModelDiagnosis(entity.getDiagnosis()));
        return model;
    }

    /**
     * Converts an API diagnosis into the embeddable entity type.
     *
     * @param input the API diagnosis, may be {@code null}
     * @return the entity diagnosis, or {@code null} if none was supplied
     */
    public static Diagnosis toEntityDiagnosis(org.openapitools.model.Diagnosis input) {
        if (input == null) {
            return null;
        }
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setCode(input.getCode());
        diagnosis.setDescription(input.getDescription());
        return diagnosis;
    }

    private static org.openapitools.model.Diagnosis toModelDiagnosis(Diagnosis entity) {
        if (entity == null) {
            return null;
        }
        return new org.openapitools.model.Diagnosis(entity.getCode(), entity.getDescription());
    }
}

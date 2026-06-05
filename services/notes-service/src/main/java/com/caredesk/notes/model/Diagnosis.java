package com.caredesk.notes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A structured diagnosis tag attached to a {@link ClinicalNote}.
 *
 * <p>Embedded into the owning note's table rather than stored separately —
 * a note has at most one diagnosis. Mirrors the {@code Diagnosis} schema in
 * the OpenAPI spec (a {@code code} such as an ICD-10 identifier plus a
 * human-readable {@code description}).
 */
@Embeddable
public class Diagnosis {

    @Column(name = "diagnosis_code")
    private String code;

    @Column(name = "diagnosis_description")
    private String description;

    /** @return the diagnosis code (for example an ICD-10 identifier) */
    public String getCode() { return code; }

    /** @param code the diagnosis code (for example an ICD-10 identifier) */
    public void setCode(String code) { this.code = code; }

    /** @return the human-readable diagnosis description */
    public String getDescription() { return description; }

    /** @param description the human-readable diagnosis description */
    public void setDescription(String description) { this.description = description; }
}

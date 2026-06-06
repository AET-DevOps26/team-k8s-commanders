package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.openapitools.model.Diagnosis;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ClinicalNote
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:57:29.113214737+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class ClinicalNote {

  private UUID id;

  private UUID appointmentId;

  private UUID doctorId;

  private String content;

  private @Nullable Diagnosis diagnosis;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public ClinicalNote() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClinicalNote(UUID id, UUID appointmentId, UUID doctorId, String content, OffsetDateTime createdAt) {
    this.id = id;
    this.appointmentId = appointmentId;
    this.doctorId = doctorId;
    this.content = content;
    this.createdAt = createdAt;
  }

  public ClinicalNote id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull @Valid 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public ClinicalNote appointmentId(UUID appointmentId) {
    this.appointmentId = appointmentId;
    return this;
  }

  /**
   * Get appointmentId
   * @return appointmentId
   */
  @NotNull @Valid 
  @Schema(name = "appointmentId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appointmentId")
  public UUID getAppointmentId() {
    return appointmentId;
  }

  @JsonProperty("appointmentId")
  public void setAppointmentId(UUID appointmentId) {
    this.appointmentId = appointmentId;
  }

  public ClinicalNote doctorId(UUID doctorId) {
    this.doctorId = doctorId;
    return this;
  }

  /**
   * Get doctorId
   * @return doctorId
   */
  @NotNull @Valid 
  @Schema(name = "doctorId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("doctorId")
  public UUID getDoctorId() {
    return doctorId;
  }

  @JsonProperty("doctorId")
  public void setDoctorId(UUID doctorId) {
    this.doctorId = doctorId;
  }

  public ClinicalNote content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public ClinicalNote diagnosis(@Nullable Diagnosis diagnosis) {
    this.diagnosis = diagnosis;
    return this;
  }

  /**
   * Get diagnosis
   * @return diagnosis
   */
  @Valid 
  @Schema(name = "diagnosis", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("diagnosis")
  public @Nullable Diagnosis getDiagnosis() {
    return diagnosis;
  }

  @JsonProperty("diagnosis")
  public void setDiagnosis(@Nullable Diagnosis diagnosis) {
    this.diagnosis = diagnosis;
  }

  public ClinicalNote createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("createdAt")
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClinicalNote clinicalNote = (ClinicalNote) o;
    return Objects.equals(this.id, clinicalNote.id) &&
        Objects.equals(this.appointmentId, clinicalNote.appointmentId) &&
        Objects.equals(this.doctorId, clinicalNote.doctorId) &&
        Objects.equals(this.content, clinicalNote.content) &&
        Objects.equals(this.diagnosis, clinicalNote.diagnosis) &&
        Objects.equals(this.createdAt, clinicalNote.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appointmentId, doctorId, content, diagnosis, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClinicalNote {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appointmentId: ").append(toIndentedString(appointmentId)).append("\n");
    sb.append("    doctorId: ").append(toIndentedString(doctorId)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    diagnosis: ").append(toIndentedString(diagnosis)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}


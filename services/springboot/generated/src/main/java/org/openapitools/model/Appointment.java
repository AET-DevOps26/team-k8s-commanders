package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.openapitools.model.AppointmentStatus;
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
 * Appointment
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:28:09.954288002+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class Appointment {

  private UUID id;

  private UUID patientId;

  private UUID doctorId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dateTime;

  private AppointmentStatus status;

  private Integer duration;

  private @Nullable String reason;

  public Appointment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Appointment(UUID id, UUID patientId, UUID doctorId, OffsetDateTime dateTime, AppointmentStatus status, Integer duration) {
    this.id = id;
    this.patientId = patientId;
    this.doctorId = doctorId;
    this.dateTime = dateTime;
    this.status = status;
    this.duration = duration;
  }

  public Appointment id(UUID id) {
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

  public Appointment patientId(UUID patientId) {
    this.patientId = patientId;
    return this;
  }

  /**
   * Get patientId
   * @return patientId
   */
  @NotNull @Valid 
  @Schema(name = "patientId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("patientId")
  public UUID getPatientId() {
    return patientId;
  }

  @JsonProperty("patientId")
  public void setPatientId(UUID patientId) {
    this.patientId = patientId;
  }

  public Appointment doctorId(UUID doctorId) {
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

  public Appointment dateTime(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
    return this;
  }

  /**
   * Get dateTime
   * @return dateTime
   */
  @NotNull @Valid 
  @Schema(name = "dateTime", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("dateTime")
  public OffsetDateTime getDateTime() {
    return dateTime;
  }

  @JsonProperty("dateTime")
  public void setDateTime(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public Appointment status(AppointmentStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public AppointmentStatus getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }

  public Appointment duration(Integer duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Duration in minutes
   * @return duration
   */
  @NotNull 
  @Schema(name = "duration", description = "Duration in minutes", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("duration")
  public Integer getDuration() {
    return duration;
  }

  @JsonProperty("duration")
  public void setDuration(Integer duration) {
    this.duration = duration;
  }

  public Appointment reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Get reason
   * @return reason
   */
  
  @Schema(name = "reason", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Appointment appointment = (Appointment) o;
    return Objects.equals(this.id, appointment.id) &&
        Objects.equals(this.patientId, appointment.patientId) &&
        Objects.equals(this.doctorId, appointment.doctorId) &&
        Objects.equals(this.dateTime, appointment.dateTime) &&
        Objects.equals(this.status, appointment.status) &&
        Objects.equals(this.duration, appointment.duration) &&
        Objects.equals(this.reason, appointment.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, patientId, doctorId, dateTime, status, duration, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Appointment {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    doctorId: ").append(toIndentedString(doctorId)).append("\n");
    sb.append("    dateTime: ").append(toIndentedString(dateTime)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    duration: ").append(toIndentedString(duration)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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


package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
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
 * AppointmentCreate
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:57:29.113214737+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class AppointmentCreate {

  private UUID patientId;

  private UUID doctorId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dateTime;

  private Integer duration;

  private @Nullable String reason;

  public AppointmentCreate() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppointmentCreate(UUID patientId, UUID doctorId, OffsetDateTime dateTime, Integer duration) {
    this.patientId = patientId;
    this.doctorId = doctorId;
    this.dateTime = dateTime;
    this.duration = duration;
  }

  public AppointmentCreate patientId(UUID patientId) {
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

  public AppointmentCreate doctorId(UUID doctorId) {
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

  public AppointmentCreate dateTime(OffsetDateTime dateTime) {
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

  public AppointmentCreate duration(Integer duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Get duration
   * minimum: 1
   * @return duration
   */
  @NotNull @Min(value = 1) 
  @Schema(name = "duration", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("duration")
  public Integer getDuration() {
    return duration;
  }

  @JsonProperty("duration")
  public void setDuration(Integer duration) {
    this.duration = duration;
  }

  public AppointmentCreate reason(@Nullable String reason) {
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
    AppointmentCreate appointmentCreate = (AppointmentCreate) o;
    return Objects.equals(this.patientId, appointmentCreate.patientId) &&
        Objects.equals(this.doctorId, appointmentCreate.doctorId) &&
        Objects.equals(this.dateTime, appointmentCreate.dateTime) &&
        Objects.equals(this.duration, appointmentCreate.duration) &&
        Objects.equals(this.reason, appointmentCreate.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patientId, doctorId, dateTime, duration, reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppointmentCreate {\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    doctorId: ").append(toIndentedString(doctorId)).append("\n");
    sb.append("    dateTime: ").append(toIndentedString(dateTime)).append("\n");
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


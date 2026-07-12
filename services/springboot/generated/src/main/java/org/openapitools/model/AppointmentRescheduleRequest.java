package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AppointmentRescheduleRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class AppointmentRescheduleRequest {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dateTime;

  private @Nullable Integer duration;

  public AppointmentRescheduleRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppointmentRescheduleRequest(OffsetDateTime dateTime) {
    this.dateTime = dateTime;
  }

  public AppointmentRescheduleRequest dateTime(OffsetDateTime dateTime) {
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

  public AppointmentRescheduleRequest duration(@Nullable Integer duration) {
    this.duration = duration;
    return this;
  }

  /**
   * Get duration
   * @return duration
   */
  
  @Schema(name = "duration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("duration")
  public @Nullable Integer getDuration() {
    return duration;
  }

  @JsonProperty("duration")
  public void setDuration(@Nullable Integer duration) {
    this.duration = duration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppointmentRescheduleRequest appointmentRescheduleRequest = (AppointmentRescheduleRequest) o;
    return Objects.equals(this.dateTime, appointmentRescheduleRequest.dateTime) &&
        Objects.equals(this.duration, appointmentRescheduleRequest.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTime, duration);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppointmentRescheduleRequest {\n");
    sb.append("    dateTime: ").append(toIndentedString(dateTime)).append("\n");
    sb.append("    duration: ").append(toIndentedString(duration)).append("\n");
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


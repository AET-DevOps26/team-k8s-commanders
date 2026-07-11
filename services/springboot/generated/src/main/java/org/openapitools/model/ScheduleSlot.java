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
 * ScheduleSlot
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class ScheduleSlot {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime endAt;

  private Boolean available;

  public ScheduleSlot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScheduleSlot(OffsetDateTime startAt, OffsetDateTime endAt, Boolean available) {
    this.startAt = startAt;
    this.endAt = endAt;
    this.available = available;
  }

  public ScheduleSlot startAt(OffsetDateTime startAt) {
    this.startAt = startAt;
    return this;
  }

  /**
   * Get startAt
   * @return startAt
   */
  @NotNull @Valid 
  @Schema(name = "startAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startAt")
  public OffsetDateTime getStartAt() {
    return startAt;
  }

  @JsonProperty("startAt")
  public void setStartAt(OffsetDateTime startAt) {
    this.startAt = startAt;
  }

  public ScheduleSlot endAt(OffsetDateTime endAt) {
    this.endAt = endAt;
    return this;
  }

  /**
   * Get endAt
   * @return endAt
   */
  @NotNull @Valid 
  @Schema(name = "endAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("endAt")
  public OffsetDateTime getEndAt() {
    return endAt;
  }

  @JsonProperty("endAt")
  public void setEndAt(OffsetDateTime endAt) {
    this.endAt = endAt;
  }

  public ScheduleSlot available(Boolean available) {
    this.available = available;
    return this;
  }

  /**
   * Get available
   * @return available
   */
  @NotNull 
  @Schema(name = "available", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("available")
  public Boolean getAvailable() {
    return available;
  }

  @JsonProperty("available")
  public void setAvailable(Boolean available) {
    this.available = available;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScheduleSlot scheduleSlot = (ScheduleSlot) o;
    return Objects.equals(this.startAt, scheduleSlot.startAt) &&
        Objects.equals(this.endAt, scheduleSlot.endAt) &&
        Objects.equals(this.available, scheduleSlot.available);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startAt, endAt, available);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScheduleSlot {\n");
    sb.append("    startAt: ").append(toIndentedString(startAt)).append("\n");
    sb.append("    endAt: ").append(toIndentedString(endAt)).append("\n");
    sb.append("    available: ").append(toIndentedString(available)).append("\n");
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


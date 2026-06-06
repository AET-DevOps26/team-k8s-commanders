package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.ScheduleSlot;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Schedule
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:28:09.954288002+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class Schedule {

  private UUID doctorId;

  @Valid
  private List<@Valid ScheduleSlot> slots = new ArrayList<>();

  public Schedule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Schedule(UUID doctorId, List<@Valid ScheduleSlot> slots) {
    this.doctorId = doctorId;
    this.slots = slots;
  }

  public Schedule doctorId(UUID doctorId) {
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

  public Schedule slots(List<@Valid ScheduleSlot> slots) {
    this.slots = slots;
    return this;
  }

  public Schedule addSlotsItem(ScheduleSlot slotsItem) {
    if (this.slots == null) {
      this.slots = new ArrayList<>();
    }
    this.slots.add(slotsItem);
    return this;
  }

  /**
   * Get slots
   * @return slots
   */
  @NotNull @Valid 
  @Schema(name = "slots", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("slots")
  public List<@Valid ScheduleSlot> getSlots() {
    return slots;
  }

  @JsonProperty("slots")
  public void setSlots(List<@Valid ScheduleSlot> slots) {
    this.slots = slots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Schedule schedule = (Schedule) o;
    return Objects.equals(this.doctorId, schedule.doctorId) &&
        Objects.equals(this.slots, schedule.slots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(doctorId, slots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Schedule {\n");
    sb.append("    doctorId: ").append(toIndentedString(doctorId)).append("\n");
    sb.append("    slots: ").append(toIndentedString(slots)).append("\n");
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


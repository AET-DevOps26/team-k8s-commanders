package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.ScheduleSlot;
import org.openapitools.model.SlotInterval;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * RecurringScheduleResult
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class RecurringScheduleResult {

  @Valid
  private List<@Valid ScheduleSlot> created = new ArrayList<>();

  @Valid
  private List<@Valid SlotInterval> skipped = new ArrayList<>();

  public RecurringScheduleResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecurringScheduleResult(List<@Valid ScheduleSlot> created, List<@Valid SlotInterval> skipped) {
    this.created = created;
    this.skipped = skipped;
  }

  public RecurringScheduleResult created(List<@Valid ScheduleSlot> created) {
    this.created = created;
    return this;
  }

  public RecurringScheduleResult addCreatedItem(ScheduleSlot createdItem) {
    if (this.created == null) {
      this.created = new ArrayList<>();
    }
    this.created.add(createdItem);
    return this;
  }

  /**
   * Get created
   * @return created
   */
  @NotNull @Valid 
  @Schema(name = "created", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("created")
  public List<@Valid ScheduleSlot> getCreated() {
    return created;
  }

  @JsonProperty("created")
  public void setCreated(List<@Valid ScheduleSlot> created) {
    this.created = created;
  }

  public RecurringScheduleResult skipped(List<@Valid SlotInterval> skipped) {
    this.skipped = skipped;
    return this;
  }

  public RecurringScheduleResult addSkippedItem(SlotInterval skippedItem) {
    if (this.skipped == null) {
      this.skipped = new ArrayList<>();
    }
    this.skipped.add(skippedItem);
    return this;
  }

  /**
   * Occurrences not created because they overlap existing slots.
   * @return skipped
   */
  @NotNull @Valid 
  @Schema(name = "skipped", description = "Occurrences not created because they overlap existing slots.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("skipped")
  public List<@Valid SlotInterval> getSkipped() {
    return skipped;
  }

  @JsonProperty("skipped")
  public void setSkipped(List<@Valid SlotInterval> skipped) {
    this.skipped = skipped;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecurringScheduleResult recurringScheduleResult = (RecurringScheduleResult) o;
    return Objects.equals(this.created, recurringScheduleResult.created) &&
        Objects.equals(this.skipped, recurringScheduleResult.skipped);
  }

  @Override
  public int hashCode() {
    return Objects.hash(created, skipped);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecurringScheduleResult {\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    skipped: ").append(toIndentedString(skipped)).append("\n");
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


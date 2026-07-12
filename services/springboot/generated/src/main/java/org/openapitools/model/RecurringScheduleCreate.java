package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openapitools.model.Weekday;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * RecurringScheduleCreate
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class RecurringScheduleCreate {

  @Valid
  private Set<Weekday> weekdays = new LinkedHashSet<>();

  private String startTime;

  private String endTime;

  /**
   * Gets or Sets slotDurationMinutes
   */
  public enum SlotDurationMinutesEnum {
    NUMBER_15(15),
    
    NUMBER_30(30),
    
    NUMBER_45(45),
    
    NUMBER_60(60);

    private final Integer value;

    SlotDurationMinutesEnum(Integer value) {
      this.value = value;
    }

    @JsonValue
    public Integer getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SlotDurationMinutesEnum fromValue(Integer value) {
      for (SlotDurationMinutesEnum b : SlotDurationMinutesEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private SlotDurationMinutesEnum slotDurationMinutes;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  private String timezone;

  public RecurringScheduleCreate() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecurringScheduleCreate(Set<Weekday> weekdays, String startTime, String endTime, SlotDurationMinutesEnum slotDurationMinutes, LocalDate startDate, LocalDate endDate, String timezone) {
    this.weekdays = weekdays;
    this.startTime = startTime;
    this.endTime = endTime;
    this.slotDurationMinutes = slotDurationMinutes;
    this.startDate = startDate;
    this.endDate = endDate;
    this.timezone = timezone;
  }

  public RecurringScheduleCreate weekdays(Set<Weekday> weekdays) {
    this.weekdays = weekdays;
    return this;
  }

  public RecurringScheduleCreate addWeekdaysItem(Weekday weekdaysItem) {
    if (this.weekdays == null) {
      this.weekdays = new LinkedHashSet<>();
    }
    this.weekdays.add(weekdaysItem);
    return this;
  }

  /**
   * Get weekdays
   * @return weekdays
   */
  @NotNull @Valid @Size(min = 1) 
  @Schema(name = "weekdays", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("weekdays")
  public Set<Weekday> getWeekdays() {
    return weekdays;
  }

  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty("weekdays")
  public void setWeekdays(Set<Weekday> weekdays) {
    this.weekdays = weekdays;
  }

  public RecurringScheduleCreate startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Daily window start (wall clock, HH:mm).
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") 
  @Schema(name = "startTime", example = "09:00", description = "Daily window start (wall clock, HH:mm).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public RecurringScheduleCreate endTime(String endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * Daily window end (wall clock, HH:mm, exclusive).
   * @return endTime
   */
  @NotNull @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$") 
  @Schema(name = "endTime", example = "12:00", description = "Daily window end (wall clock, HH:mm, exclusive).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("endTime")
  public String getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public RecurringScheduleCreate slotDurationMinutes(SlotDurationMinutesEnum slotDurationMinutes) {
    this.slotDurationMinutes = slotDurationMinutes;
    return this;
  }

  /**
   * Get slotDurationMinutes
   * @return slotDurationMinutes
   */
  @NotNull 
  @Schema(name = "slotDurationMinutes", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("slotDurationMinutes")
  public SlotDurationMinutesEnum getSlotDurationMinutes() {
    return slotDurationMinutes;
  }

  @JsonProperty("slotDurationMinutes")
  public void setSlotDurationMinutes(SlotDurationMinutesEnum slotDurationMinutes) {
    this.slotDurationMinutes = slotDurationMinutes;
  }

  public RecurringScheduleCreate startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Get startDate
   * @return startDate
   */
  @NotNull @Valid 
  @Schema(name = "startDate", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startDate")
  public LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public RecurringScheduleCreate endDate(LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Inclusive; at most 12 weeks after startDate.
   * @return endDate
   */
  @NotNull @Valid 
  @Schema(name = "endDate", description = "Inclusive; at most 12 weeks after startDate.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("endDate")
  public LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public RecurringScheduleCreate timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * IANA zone id used for wall-clock expansion.
   * @return timezone
   */
  @NotNull 
  @Schema(name = "timezone", example = "Europe/Berlin", description = "IANA zone id used for wall-clock expansion.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecurringScheduleCreate recurringScheduleCreate = (RecurringScheduleCreate) o;
    return Objects.equals(this.weekdays, recurringScheduleCreate.weekdays) &&
        Objects.equals(this.startTime, recurringScheduleCreate.startTime) &&
        Objects.equals(this.endTime, recurringScheduleCreate.endTime) &&
        Objects.equals(this.slotDurationMinutes, recurringScheduleCreate.slotDurationMinutes) &&
        Objects.equals(this.startDate, recurringScheduleCreate.startDate) &&
        Objects.equals(this.endDate, recurringScheduleCreate.endDate) &&
        Objects.equals(this.timezone, recurringScheduleCreate.timezone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(weekdays, startTime, endTime, slotDurationMinutes, startDate, endDate, timezone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecurringScheduleCreate {\n");
    sb.append("    weekdays: ").append(toIndentedString(weekdays)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    slotDurationMinutes: ").append(toIndentedString(slotDurationMinutes)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
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


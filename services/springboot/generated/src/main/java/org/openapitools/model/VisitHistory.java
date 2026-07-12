package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.Appointment;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * VisitHistory
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class VisitHistory {

  private UUID patientId;

  @Valid
  private List<@Valid Appointment> appointments = new ArrayList<>();

  public VisitHistory() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VisitHistory(UUID patientId, List<@Valid Appointment> appointments) {
    this.patientId = patientId;
    this.appointments = appointments;
  }

  public VisitHistory patientId(UUID patientId) {
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

  public VisitHistory appointments(List<@Valid Appointment> appointments) {
    this.appointments = appointments;
    return this;
  }

  public VisitHistory addAppointmentsItem(Appointment appointmentsItem) {
    if (this.appointments == null) {
      this.appointments = new ArrayList<>();
    }
    this.appointments.add(appointmentsItem);
    return this;
  }

  /**
   * Get appointments
   * @return appointments
   */
  @NotNull @Valid 
  @Schema(name = "appointments", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appointments")
  public List<@Valid Appointment> getAppointments() {
    return appointments;
  }

  @JsonProperty("appointments")
  public void setAppointments(List<@Valid Appointment> appointments) {
    this.appointments = appointments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VisitHistory visitHistory = (VisitHistory) o;
    return Objects.equals(this.patientId, visitHistory.patientId) &&
        Objects.equals(this.appointments, visitHistory.appointments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patientId, appointments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VisitHistory {\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    appointments: ").append(toIndentedString(appointments)).append("\n");
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


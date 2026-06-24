package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UserStats
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class UserStats {

  private Long total;

  private Long patients;

  private Long doctors;

  private Long admins;

  private Long active;

  private Long disabled;

  public UserStats() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserStats(Long total, Long patients, Long doctors, Long admins, Long active, Long disabled) {
    this.total = total;
    this.patients = patients;
    this.doctors = doctors;
    this.admins = admins;
    this.active = active;
    this.disabled = disabled;
  }

  public UserStats total(Long total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull 
  @Schema(name = "total", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Long getTotal() {
    return total;
  }

  @JsonProperty("total")
  public void setTotal(Long total) {
    this.total = total;
  }

  public UserStats patients(Long patients) {
    this.patients = patients;
    return this;
  }

  /**
   * Get patients
   * @return patients
   */
  @NotNull 
  @Schema(name = "patients", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("patients")
  public Long getPatients() {
    return patients;
  }

  @JsonProperty("patients")
  public void setPatients(Long patients) {
    this.patients = patients;
  }

  public UserStats doctors(Long doctors) {
    this.doctors = doctors;
    return this;
  }

  /**
   * Get doctors
   * @return doctors
   */
  @NotNull 
  @Schema(name = "doctors", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("doctors")
  public Long getDoctors() {
    return doctors;
  }

  @JsonProperty("doctors")
  public void setDoctors(Long doctors) {
    this.doctors = doctors;
  }

  public UserStats admins(Long admins) {
    this.admins = admins;
    return this;
  }

  /**
   * Get admins
   * @return admins
   */
  @NotNull 
  @Schema(name = "admins", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("admins")
  public Long getAdmins() {
    return admins;
  }

  @JsonProperty("admins")
  public void setAdmins(Long admins) {
    this.admins = admins;
  }

  public UserStats active(Long active) {
    this.active = active;
    return this;
  }

  /**
   * Get active
   * @return active
   */
  @NotNull 
  @Schema(name = "active", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("active")
  public Long getActive() {
    return active;
  }

  @JsonProperty("active")
  public void setActive(Long active) {
    this.active = active;
  }

  public UserStats disabled(Long disabled) {
    this.disabled = disabled;
    return this;
  }

  /**
   * Get disabled
   * @return disabled
   */
  @NotNull 
  @Schema(name = "disabled", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("disabled")
  public Long getDisabled() {
    return disabled;
  }

  @JsonProperty("disabled")
  public void setDisabled(Long disabled) {
    this.disabled = disabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserStats userStats = (UserStats) o;
    return Objects.equals(this.total, userStats.total) &&
        Objects.equals(this.patients, userStats.patients) &&
        Objects.equals(this.doctors, userStats.doctors) &&
        Objects.equals(this.admins, userStats.admins) &&
        Objects.equals(this.active, userStats.active) &&
        Objects.equals(this.disabled, userStats.disabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(total, patients, doctors, admins, active, disabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserStats {\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    patients: ").append(toIndentedString(patients)).append("\n");
    sb.append("    doctors: ").append(toIndentedString(doctors)).append("\n");
    sb.append("    admins: ").append(toIndentedString(admins)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    disabled: ").append(toIndentedString(disabled)).append("\n");
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


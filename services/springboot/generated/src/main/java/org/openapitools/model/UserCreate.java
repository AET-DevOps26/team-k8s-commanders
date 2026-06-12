package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.openapitools.model.UserRole;
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
 * UserCreate
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class UserCreate {

  private String name;

  private String email;

  private String password;

  private UserRole role;

  private @Nullable String phoneNumber;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateOfBirth;

  private @Nullable String specialization;

  private @Nullable String licenseNumber;

  private @Nullable UUID clinicId;

  public UserCreate() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserCreate(String name, String email, String password, UserRole role) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  public UserCreate name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public UserCreate email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   * @return email
   */
  @NotNull @jakarta.validation.constraints.Email 
  @Schema(name = "email", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  public UserCreate password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
   */
  @NotNull 
  @Schema(name = "password", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  @JsonProperty("password")
  public void setPassword(String password) {
    this.password = password;
  }

  public UserCreate role(UserRole role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @NotNull @Valid 
  @Schema(name = "role", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role")
  public UserRole getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(UserRole role) {
    this.role = role;
  }

  public UserCreate phoneNumber(@Nullable String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  /**
   * Get phoneNumber
   * @return phoneNumber
   */
  
  @Schema(name = "phoneNumber", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("phoneNumber")
  public @Nullable String getPhoneNumber() {
    return phoneNumber;
  }

  @JsonProperty("phoneNumber")
  public void setPhoneNumber(@Nullable String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public UserCreate dateOfBirth(@Nullable LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
    return this;
  }

  /**
   * Get dateOfBirth
   * @return dateOfBirth
   */
  @Valid 
  @Schema(name = "dateOfBirth", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dateOfBirth")
  public @Nullable LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  @JsonProperty("dateOfBirth")
  public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public UserCreate specialization(@Nullable String specialization) {
    this.specialization = specialization;
    return this;
  }

  /**
   * Get specialization
   * @return specialization
   */
  
  @Schema(name = "specialization", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("specialization")
  public @Nullable String getSpecialization() {
    return specialization;
  }

  @JsonProperty("specialization")
  public void setSpecialization(@Nullable String specialization) {
    this.specialization = specialization;
  }

  public UserCreate licenseNumber(@Nullable String licenseNumber) {
    this.licenseNumber = licenseNumber;
    return this;
  }

  /**
   * Get licenseNumber
   * @return licenseNumber
   */
  
  @Schema(name = "licenseNumber", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("licenseNumber")
  public @Nullable String getLicenseNumber() {
    return licenseNumber;
  }

  @JsonProperty("licenseNumber")
  public void setLicenseNumber(@Nullable String licenseNumber) {
    this.licenseNumber = licenseNumber;
  }

  public UserCreate clinicId(@Nullable UUID clinicId) {
    this.clinicId = clinicId;
    return this;
  }

  /**
   * Get clinicId
   * @return clinicId
   */
  @Valid 
  @Schema(name = "clinicId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("clinicId")
  public @Nullable UUID getClinicId() {
    return clinicId;
  }

  @JsonProperty("clinicId")
  public void setClinicId(@Nullable UUID clinicId) {
    this.clinicId = clinicId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserCreate userCreate = (UserCreate) o;
    return Objects.equals(this.name, userCreate.name) &&
        Objects.equals(this.email, userCreate.email) &&
        Objects.equals(this.password, userCreate.password) &&
        Objects.equals(this.role, userCreate.role) &&
        Objects.equals(this.phoneNumber, userCreate.phoneNumber) &&
        Objects.equals(this.dateOfBirth, userCreate.dateOfBirth) &&
        Objects.equals(this.specialization, userCreate.specialization) &&
        Objects.equals(this.licenseNumber, userCreate.licenseNumber) &&
        Objects.equals(this.clinicId, userCreate.clinicId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, email, password, role, phoneNumber, dateOfBirth, specialization, licenseNumber, clinicId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserCreate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    password: ").append("*").append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    phoneNumber: ").append(toIndentedString(phoneNumber)).append("\n");
    sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
    sb.append("    specialization: ").append(toIndentedString(specialization)).append("\n");
    sb.append("    licenseNumber: ").append(toIndentedString(licenseNumber)).append("\n");
    sb.append("    clinicId: ").append(toIndentedString(clinicId)).append("\n");
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


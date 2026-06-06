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
 * UserProfile
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:28:09.954288002+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class UserProfile {

  private UUID id;

  private String name;

  private String email;

  private UserRole role;

  private @Nullable UUID clinicId;

  private @Nullable String phoneNumber;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateOfBirth;

  private @Nullable String specialization;

  private @Nullable String licenseNumber;

  private @Nullable String password;

  public UserProfile() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserProfile(UUID id, String name, String email, UserRole role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.role = role;
  }

  public UserProfile id(UUID id) {
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

  public UserProfile name(String name) {
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

  public UserProfile email(String email) {
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

  public UserProfile role(UserRole role) {
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

  public UserProfile clinicId(@Nullable UUID clinicId) {
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

  public UserProfile phoneNumber(@Nullable String phoneNumber) {
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

  public UserProfile dateOfBirth(@Nullable LocalDate dateOfBirth) {
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

  public UserProfile specialization(@Nullable String specialization) {
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

  public UserProfile licenseNumber(@Nullable String licenseNumber) {
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

  public UserProfile password(@Nullable String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
   */
  
  @Schema(name = "password", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("password")
  public @Nullable String getPassword() {
    return password;
  }

  @JsonProperty("password")
  public void setPassword(@Nullable String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserProfile userProfile = (UserProfile) o;
    return Objects.equals(this.id, userProfile.id) &&
        Objects.equals(this.name, userProfile.name) &&
        Objects.equals(this.email, userProfile.email) &&
        Objects.equals(this.role, userProfile.role) &&
        Objects.equals(this.clinicId, userProfile.clinicId) &&
        Objects.equals(this.phoneNumber, userProfile.phoneNumber) &&
        Objects.equals(this.dateOfBirth, userProfile.dateOfBirth) &&
        Objects.equals(this.specialization, userProfile.specialization) &&
        Objects.equals(this.licenseNumber, userProfile.licenseNumber) &&
        Objects.equals(this.password, userProfile.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, role, clinicId, phoneNumber, dateOfBirth, specialization, licenseNumber, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserProfile {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    clinicId: ").append(toIndentedString(clinicId)).append("\n");
    sb.append("    phoneNumber: ").append(toIndentedString(phoneNumber)).append("\n");
    sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
    sb.append("    specialization: ").append(toIndentedString(specialization)).append("\n");
    sb.append("    licenseNumber: ").append(toIndentedString(licenseNumber)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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


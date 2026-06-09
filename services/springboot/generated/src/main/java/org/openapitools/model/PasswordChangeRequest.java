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
 * PasswordChangeRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class PasswordChangeRequest {

  private String currentPassword;

  private String newPassword;

  public PasswordChangeRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PasswordChangeRequest(String currentPassword, String newPassword) {
    this.currentPassword = currentPassword;
    this.newPassword = newPassword;
  }

  public PasswordChangeRequest currentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
    return this;
  }

  /**
   * Get currentPassword
   * @return currentPassword
   */
  @NotNull 
  @Schema(name = "currentPassword", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("currentPassword")
  public String getCurrentPassword() {
    return currentPassword;
  }

  @JsonProperty("currentPassword")
  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public PasswordChangeRequest newPassword(String newPassword) {
    this.newPassword = newPassword;
    return this;
  }

  /**
   * Get newPassword
   * @return newPassword
   */
  @NotNull @Size(min = 8) 
  @Schema(name = "newPassword", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("newPassword")
  public String getNewPassword() {
    return newPassword;
  }

  @JsonProperty("newPassword")
  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PasswordChangeRequest passwordChangeRequest = (PasswordChangeRequest) o;
    return Objects.equals(this.currentPassword, passwordChangeRequest.currentPassword) &&
        Objects.equals(this.newPassword, passwordChangeRequest.newPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentPassword, newPassword);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PasswordChangeRequest {\n");
    sb.append("    currentPassword: ").append("*").append("\n");
    sb.append("    newPassword: ").append("*").append("\n");
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


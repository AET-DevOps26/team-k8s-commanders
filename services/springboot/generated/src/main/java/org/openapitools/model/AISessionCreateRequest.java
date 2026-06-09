package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Options for a new conversation. Binding a patient and/or appointment grounds every message in the session in that record&#39;s live data. 
 */

@Schema(name = "AISessionCreateRequest", description = "Options for a new conversation. Binding a patient and/or appointment grounds every message in the session in that record's live data. ")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class AISessionCreateRequest {

  private @Nullable UUID patientId;

  private @Nullable UUID appointmentId;

  private @Nullable String title;

  public AISessionCreateRequest patientId(@Nullable UUID patientId) {
    this.patientId = patientId;
    return this;
  }

  /**
   * Get patientId
   * @return patientId
   */
  @Valid 
  @Schema(name = "patientId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("patientId")
  public @Nullable UUID getPatientId() {
    return patientId;
  }

  @JsonProperty("patientId")
  public void setPatientId(@Nullable UUID patientId) {
    this.patientId = patientId;
  }

  public AISessionCreateRequest appointmentId(@Nullable UUID appointmentId) {
    this.appointmentId = appointmentId;
    return this;
  }

  /**
   * Get appointmentId
   * @return appointmentId
   */
  @Valid 
  @Schema(name = "appointmentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appointmentId")
  public @Nullable UUID getAppointmentId() {
    return appointmentId;
  }

  @JsonProperty("appointmentId")
  public void setAppointmentId(@Nullable UUID appointmentId) {
    this.appointmentId = appointmentId;
  }

  public AISessionCreateRequest title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Optional human-friendly label for the conversation.
   * @return title
   */
  
  @Schema(name = "title", description = "Optional human-friendly label for the conversation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AISessionCreateRequest aiSessionCreateRequest = (AISessionCreateRequest) o;
    return Objects.equals(this.patientId, aiSessionCreateRequest.patientId) &&
        Objects.equals(this.appointmentId, aiSessionCreateRequest.appointmentId) &&
        Objects.equals(this.title, aiSessionCreateRequest.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patientId, appointmentId, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AISessionCreateRequest {\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    appointmentId: ").append(toIndentedString(appointmentId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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


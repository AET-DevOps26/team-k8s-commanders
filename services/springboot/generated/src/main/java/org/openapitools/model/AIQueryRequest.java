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
 * AIQueryRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:57:29.113214737+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class AIQueryRequest {

  private @Nullable UUID patientId;

  private @Nullable UUID appointmentId;

  private String query;

  public AIQueryRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AIQueryRequest(String query) {
    this.query = query;
  }

  public AIQueryRequest patientId(@Nullable UUID patientId) {
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

  public AIQueryRequest appointmentId(@Nullable UUID appointmentId) {
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

  public AIQueryRequest query(String query) {
    this.query = query;
    return this;
  }

  /**
   * Get query
   * @return query
   */
  @NotNull 
  @Schema(name = "query", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("query")
  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AIQueryRequest aiQueryRequest = (AIQueryRequest) o;
    return Objects.equals(this.patientId, aiQueryRequest.patientId) &&
        Objects.equals(this.appointmentId, aiQueryRequest.appointmentId) &&
        Objects.equals(this.query, aiQueryRequest.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(patientId, appointmentId, query);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AIQueryRequest {\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    appointmentId: ").append(toIndentedString(appointmentId)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
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


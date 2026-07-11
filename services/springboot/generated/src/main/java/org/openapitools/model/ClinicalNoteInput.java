package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.model.Diagnosis;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ClinicalNoteInput
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class ClinicalNoteInput {

  private @Nullable String content;

  private @Nullable Diagnosis diagnosis;

  public ClinicalNoteInput content(@Nullable String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  
  @Schema(name = "content", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("content")
  public @Nullable String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(@Nullable String content) {
    this.content = content;
  }

  public ClinicalNoteInput diagnosis(@Nullable Diagnosis diagnosis) {
    this.diagnosis = diagnosis;
    return this;
  }

  /**
   * Get diagnosis
   * @return diagnosis
   */
  @Valid 
  @Schema(name = "diagnosis", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("diagnosis")
  public @Nullable Diagnosis getDiagnosis() {
    return diagnosis;
  }

  @JsonProperty("diagnosis")
  public void setDiagnosis(@Nullable Diagnosis diagnosis) {
    this.diagnosis = diagnosis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClinicalNoteInput clinicalNoteInput = (ClinicalNoteInput) o;
    return Objects.equals(this.content, clinicalNoteInput.content) &&
        Objects.equals(this.diagnosis, clinicalNoteInput.diagnosis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, diagnosis);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClinicalNoteInput {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    diagnosis: ").append(toIndentedString(diagnosis)).append("\n");
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


package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AIQueryResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class AIQueryResponse {

  private String answer;

  @Valid
  private List<String> sources = new ArrayList<>();

  private @Nullable Float confidence;

  public AIQueryResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AIQueryResponse(String answer) {
    this.answer = answer;
  }

  public AIQueryResponse answer(String answer) {
    this.answer = answer;
    return this;
  }

  /**
   * Get answer
   * @return answer
   */
  @NotNull 
  @Schema(name = "answer", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("answer")
  public String getAnswer() {
    return answer;
  }

  @JsonProperty("answer")
  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public AIQueryResponse sources(List<String> sources) {
    this.sources = sources;
    return this;
  }

  public AIQueryResponse addSourcesItem(String sourcesItem) {
    if (this.sources == null) {
      this.sources = new ArrayList<>();
    }
    this.sources.add(sourcesItem);
    return this;
  }

  /**
   * Get sources
   * @return sources
   */
  
  @Schema(name = "sources", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sources")
  public List<String> getSources() {
    return sources;
  }

  @JsonProperty("sources")
  public void setSources(List<String> sources) {
    this.sources = sources;
  }

  public AIQueryResponse confidence(@Nullable Float confidence) {
    this.confidence = confidence;
    return this;
  }

  /**
   * Get confidence
   * @return confidence
   */
  
  @Schema(name = "confidence", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("confidence")
  public @Nullable Float getConfidence() {
    return confidence;
  }

  @JsonProperty("confidence")
  public void setConfidence(@Nullable Float confidence) {
    this.confidence = confidence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AIQueryResponse aiQueryResponse = (AIQueryResponse) o;
    return Objects.equals(this.answer, aiQueryResponse.answer) &&
        Objects.equals(this.sources, aiQueryResponse.sources) &&
        Objects.equals(this.confidence, aiQueryResponse.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(answer, sources, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AIQueryResponse {\n");
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
    sb.append("    sources: ").append(toIndentedString(sources)).append("\n");
    sb.append("    confidence: ").append(toIndentedString(confidence)).append("\n");
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


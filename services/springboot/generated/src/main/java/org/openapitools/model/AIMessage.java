package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.openapitools.model.AIMessageRole;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AIMessage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class AIMessage {

  private UUID id;

  private AIMessageRole role;

  private String content;

  @Valid
  private List<String> sources = new ArrayList<>();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public AIMessage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AIMessage(UUID id, AIMessageRole role, String content, OffsetDateTime createdAt) {
    this.id = id;
    this.role = role;
    this.content = content;
    this.createdAt = createdAt;
  }

  public AIMessage id(UUID id) {
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

  public AIMessage role(AIMessageRole role) {
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
  public AIMessageRole getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(AIMessageRole role) {
    this.role = role;
  }

  public AIMessage content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public AIMessage sources(List<String> sources) {
    this.sources = sources;
    return this;
  }

  public AIMessage addSourcesItem(String sourcesItem) {
    if (this.sources == null) {
      this.sources = new ArrayList<>();
    }
    this.sources.add(sourcesItem);
    return this;
  }

  /**
   * Grounding sources cited for an assistant message.
   * @return sources
   */
  
  @Schema(name = "sources", description = "Grounding sources cited for an assistant message.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sources")
  public List<String> getSources() {
    return sources;
  }

  @JsonProperty("sources")
  public void setSources(List<String> sources) {
    this.sources = sources;
  }

  public AIMessage createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("createdAt")
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AIMessage aiMessage = (AIMessage) o;
    return Objects.equals(this.id, aiMessage.id) &&
        Objects.equals(this.role, aiMessage.role) &&
        Objects.equals(this.content, aiMessage.content) &&
        Objects.equals(this.sources, aiMessage.sources) &&
        Objects.equals(this.createdAt, aiMessage.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, role, content, sources, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AIMessage {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    sources: ").append(toIndentedString(sources)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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


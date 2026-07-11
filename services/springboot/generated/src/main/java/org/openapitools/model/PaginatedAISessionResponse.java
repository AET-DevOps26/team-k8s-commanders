package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.AISessionSummary;
import org.openapitools.model.PageMeta;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PaginatedAISessionResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class PaginatedAISessionResponse {

  @Valid
  private List<@Valid AISessionSummary> content = new ArrayList<>();

  private PageMeta page;

  public PaginatedAISessionResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PaginatedAISessionResponse(List<@Valid AISessionSummary> content, PageMeta page) {
    this.content = content;
    this.page = page;
  }

  public PaginatedAISessionResponse content(List<@Valid AISessionSummary> content) {
    this.content = content;
    return this;
  }

  public PaginatedAISessionResponse addContentItem(AISessionSummary contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull @Valid 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public List<@Valid AISessionSummary> getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(List<@Valid AISessionSummary> content) {
    this.content = content;
  }

  public PaginatedAISessionResponse page(PageMeta page) {
    this.page = page;
    return this;
  }

  /**
   * Get page
   * @return page
   */
  @NotNull @Valid 
  @Schema(name = "page", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("page")
  public PageMeta getPage() {
    return page;
  }

  @JsonProperty("page")
  public void setPage(PageMeta page) {
    this.page = page;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaginatedAISessionResponse paginatedAISessionResponse = (PaginatedAISessionResponse) o;
    return Objects.equals(this.content, paginatedAISessionResponse.content) &&
        Objects.equals(this.page, paginatedAISessionResponse.page);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, page);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaginatedAISessionResponse {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
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


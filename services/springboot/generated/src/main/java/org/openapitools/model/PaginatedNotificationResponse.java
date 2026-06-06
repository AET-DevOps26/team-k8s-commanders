package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.model.Notification;
import org.openapitools.model.PageMeta;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PaginatedNotificationResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:57:29.113214737+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class PaginatedNotificationResponse {

  @Valid
  private List<@Valid Notification> content = new ArrayList<>();

  private PageMeta page;

  public PaginatedNotificationResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PaginatedNotificationResponse(List<@Valid Notification> content, PageMeta page) {
    this.content = content;
    this.page = page;
  }

  public PaginatedNotificationResponse content(List<@Valid Notification> content) {
    this.content = content;
    return this;
  }

  public PaginatedNotificationResponse addContentItem(Notification contentItem) {
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
  public List<@Valid Notification> getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(List<@Valid Notification> content) {
    this.content = content;
  }

  public PaginatedNotificationResponse page(PageMeta page) {
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
    PaginatedNotificationResponse paginatedNotificationResponse = (PaginatedNotificationResponse) o;
    return Objects.equals(this.content, paginatedNotificationResponse.content) &&
        Objects.equals(this.page, paginatedNotificationResponse.page);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, page);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaginatedNotificationResponse {\n");
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


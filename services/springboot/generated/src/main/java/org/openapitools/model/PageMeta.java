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
 * PageMeta
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T19:57:29.113214737+02:00[Europe/Berlin]", comments = "Generator version: 7.22.0")
public class PageMeta {

  private Integer page;

  private Integer size;

  private Long totalElements;

  private Integer totalPages;

  public PageMeta() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PageMeta(Integer page, Integer size, Long totalElements, Integer totalPages) {
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }

  public PageMeta page(Integer page) {
    this.page = page;
    return this;
  }

  /**
   * Zero-based page index of the returned slice.
   * minimum: 0
   * @return page
   */
  @NotNull @Min(value = 0) 
  @Schema(name = "page", description = "Zero-based page index of the returned slice.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("page")
  public Integer getPage() {
    return page;
  }

  @JsonProperty("page")
  public void setPage(Integer page) {
    this.page = page;
  }

  public PageMeta size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * Number of items per page.
   * minimum: 1
   * @return size
   */
  @NotNull @Min(value = 1) 
  @Schema(name = "size", description = "Number of items per page.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(Integer size) {
    this.size = size;
  }

  public PageMeta totalElements(Long totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * Total number of items across all pages.
   * minimum: 0
   * @return totalElements
   */
  @NotNull @Min(value = 0L) 
  @Schema(name = "totalElements", description = "Total number of items across all pages.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("totalElements")
  public Long getTotalElements() {
    return totalElements;
  }

  @JsonProperty("totalElements")
  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public PageMeta totalPages(Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * Total number of pages.
   * minimum: 0
   * @return totalPages
   */
  @NotNull @Min(value = 0) 
  @Schema(name = "totalPages", description = "Total number of pages.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("totalPages")
  public Integer getTotalPages() {
    return totalPages;
  }

  @JsonProperty("totalPages")
  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageMeta pageMeta = (PageMeta) o;
    return Objects.equals(this.page, pageMeta.page) &&
        Objects.equals(this.size, pageMeta.size) &&
        Objects.equals(this.totalElements, pageMeta.totalElements) &&
        Objects.equals(this.totalPages, pageMeta.totalPages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(page, size, totalElements, totalPages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageMeta {\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
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


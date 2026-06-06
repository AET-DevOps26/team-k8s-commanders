package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.openapitools.model.NotificationChannel;
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
 * Notification
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class Notification {

  private UUID id;

  private @Nullable UUID appointmentId;

  private @Nullable UUID patientId;

  private String message;

  private NotificationChannel channel;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sentAt;

  public Notification() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Notification(UUID id, String message, NotificationChannel channel, OffsetDateTime sentAt) {
    this.id = id;
    this.message = message;
    this.channel = channel;
    this.sentAt = sentAt;
  }

  public Notification id(UUID id) {
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

  public Notification appointmentId(@Nullable UUID appointmentId) {
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

  public Notification patientId(@Nullable UUID patientId) {
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

  public Notification message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @Schema(name = "message", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  public Notification channel(NotificationChannel channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Get channel
   * @return channel
   */
  @NotNull @Valid 
  @Schema(name = "channel", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("channel")
  public NotificationChannel getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(NotificationChannel channel) {
    this.channel = channel;
  }

  public Notification sentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * Get sentAt
   * @return sentAt
   */
  @NotNull @Valid 
  @Schema(name = "sentAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("sentAt")
  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  @JsonProperty("sentAt")
  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Notification notification = (Notification) o;
    return Objects.equals(this.id, notification.id) &&
        Objects.equals(this.appointmentId, notification.appointmentId) &&
        Objects.equals(this.patientId, notification.patientId) &&
        Objects.equals(this.message, notification.message) &&
        Objects.equals(this.channel, notification.channel) &&
        Objects.equals(this.sentAt, notification.sentAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appointmentId, patientId, message, channel, sentAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Notification {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appointmentId: ").append(toIndentedString(appointmentId)).append("\n");
    sb.append("    patientId: ").append(toIndentedString(patientId)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
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


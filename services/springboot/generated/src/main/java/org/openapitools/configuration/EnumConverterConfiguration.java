package org.openapitools.configuration;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import org.openapitools.model.AIMessageRole;
import org.openapitools.model.AppointmentStatus;
import org.openapitools.model.NotificationChannel;
import org.openapitools.model.UserRole;
import org.openapitools.model.Weekday;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * This class provides Spring Converter beans for the enum models in the OpenAPI specification.
 *
 * By default, Spring only converts primitive types to enums using Enum::valueOf, which can prevent
 * correct conversion if the OpenAPI specification is using an `enumPropertyNaming` other than
 * `original` or the specification has an integer enum.
 */
@Configuration(value = "org.openapitools.configuration.enumConverterConfiguration")
public class EnumConverterConfiguration {

    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.aiMessageRoleConverter")
    Converter<String, AIMessageRole> aiMessageRoleConverter() {
        return new Converter<String, AIMessageRole>() {
            @Override
            public AIMessageRole convert(String source) {
                return AIMessageRole.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.appointmentStatusConverter")
    Converter<String, AppointmentStatus> appointmentStatusConverter() {
        return new Converter<String, AppointmentStatus>() {
            @Override
            public AppointmentStatus convert(String source) {
                return AppointmentStatus.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.notificationChannelConverter")
    Converter<String, NotificationChannel> notificationChannelConverter() {
        return new Converter<String, NotificationChannel>() {
            @Override
            public NotificationChannel convert(String source) {
                return NotificationChannel.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.userRoleConverter")
    Converter<String, UserRole> userRoleConverter() {
        return new Converter<String, UserRole>() {
            @Override
            public UserRole convert(String source) {
                return UserRole.fromValue(source);
            }
        };
    }
    @Bean(name = "org.openapitools.configuration.EnumConverterConfiguration.weekdayConverter")
    Converter<String, Weekday> weekdayConverter() {
        return new Converter<String, Weekday>() {
            @Override
            public Weekday convert(String source) {
                return Weekday.fromValue(source);
            }
        };
    }

}

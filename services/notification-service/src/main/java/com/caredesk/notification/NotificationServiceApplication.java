package com.caredesk.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the CareDesk notification service.
 *
 * <p>{@code scanBasePackages} keeps Spring from picking up the generated
 * {@code org.openapitools} controllers (such as {@code NotificationsApiController})
 * from the OpenAPI stubs JAR. Our own controllers in
 * {@code com.caredesk.notification.controller} implement the API interfaces
 * directly.
 *
 * <p>{@code @EnableScheduling} powers the appointment reminder job (see
 * {@code ReminderScheduler}).
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.caredesk.notification")
public class NotificationServiceApplication {

    /**
     * Boots the Spring application context.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

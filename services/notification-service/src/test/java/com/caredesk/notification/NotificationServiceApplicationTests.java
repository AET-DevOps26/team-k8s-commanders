package com.caredesk.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import com.caredesk.notification.repository.NotificationRepository;

/**
 * Smoke test that verifies the Spring application context boots.
 *
 * <p>JPA, data-source and JPA-repository auto-configurations are excluded so
 * the test runs without a real Postgres instance. This keeps the smoke test
 * fast and free of Testcontainers dependencies. The repository is mocked so
 * the service and controller beans that depend on it can still be wired.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Skip the JPA dialect / connection so the context loads without a real DB.
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        // Keep the reminder scheduler from firing (and calling patient-service) during the smoke test.
        "notification.reminder.enabled=false"
})
class NotificationServiceApplicationTests {

    // Stands in for the JPA repository, which is not created while JPA is excluded.
    @MockitoBean
    private NotificationRepository notificationRepository;

    /**
     * Fails if the application context cannot start, which catches broken
     * bean wiring, invalid configuration and security misconfiguration early.
     */
    @Test
    void contextLoads() {
    }
}

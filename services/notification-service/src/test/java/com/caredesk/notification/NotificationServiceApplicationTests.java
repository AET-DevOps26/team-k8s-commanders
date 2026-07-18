package com.caredesk.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.caredesk.notification.repository.NotificationRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    /**
     * Fails if the application context cannot start, which catches broken
     * bean wiring, invalid configuration and security misconfiguration early.
     */
    @Test
    void contextLoads() {
    }

    @Test
    void unauthenticatedRequestReturnsProblemDetails() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        mvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required"));
    }
}

package com.caredesk.notes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import com.caredesk.notes.repository.ClinicalNoteRepository;

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
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class NotesServiceApplicationTests {

    // Stands in for the JPA repository, which is not created while JPA is excluded.
    @MockBean
    private ClinicalNoteRepository clinicalNoteRepository;

    /**
     * Boots the Spring context with the auto-configurations above excluded.
     * Fails the build if any bean cannot be created.
     */
    @Test
    void contextLoads() {
    }
}

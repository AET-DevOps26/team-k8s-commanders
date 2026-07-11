package com.caredesk.patient;

import com.caredesk.patient.repository.AppointmentRepository;
import com.caredesk.patient.repository.DoctorSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring application context boots.
 *
 * <p>JPA, data-source and JPA-repository auto-configurations are excluded so
 * the test runs without a real Postgres instance. The repositories are
 * provided as mocks so beans that depend on them (PatientService,
 * AppointmentService and friends) can still be created.
 */
@SpringBootTest
@TestPropertySource(properties = {
        // Skip the JPA dialect / connection so the context loads without a real DB.
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class PatientServiceApplicationTests {

    @MockitoBean
    AppointmentRepository appointmentRepository;

    @MockitoBean
    DoctorSlotRepository doctorSlotRepository;

    /**
     * Boots the Spring context with the auto-configurations above excluded.
     * Fails the build if any bean cannot be created.
     */
    @Test
    void contextLoads() {
    }
}

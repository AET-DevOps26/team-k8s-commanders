package com.caredesk.patient.config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.caredesk.patient.model.Patient;
import com.caredesk.patient.repository.PatientRepository;

/**
 * Optional dev-only clinical profiles for the 15 disposable test patients
 * seeded by auth-service {@code DevTestPatientsSeeder}.
 *
 * <p>Disable together with auth-service via {@code caredesk.seed.test-patients=false}.
 * To remove seeded rows from an existing database:
 *
 * <pre>{@code
 * DELETE FROM patients WHERE id::text LIKE 'bbbbbbb1-1111-1111-1111-%';
 * }</pre>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "caredesk.seed", name = "test-patients", havingValue = "true")
public class DevTestPatientsSeeder implements ApplicationRunner {

    static final int TEST_PATIENT_COUNT = 15;

    private static final List<TestPatientProfile> TEST_PATIENTS = buildTestPatients();

    private final PatientRepository patientRepository;

    public DevTestPatientsSeeder(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        TEST_PATIENTS.forEach(this::upsert);
    }

    private void upsert(TestPatientProfile testPatient) {
        Patient patient = patientRepository.findById(testPatient.id()).orElseGet(Patient::new);
        patient.setId(testPatient.id());
        patient.setPhoneNumber(testPatient.phoneNumber());
        patient.setDateOfBirth(testPatient.dateOfBirth());
        patientRepository.save(patient);
    }

    private static List<TestPatientProfile> buildTestPatients() {
        return java.util.stream.IntStream.rangeClosed(1, TEST_PATIENT_COUNT)
                .mapToObj(index -> new TestPatientProfile(
                        DevTestPatientIds.id(index),
                        "+49 89 200" + String.format("%03d", index),
                        LocalDate.parse("1985-01-01").plusDays(index - 1L)))
                .toList();
    }

    record TestPatientProfile(java.util.UUID id, String phoneNumber, LocalDate dateOfBirth) {
    }
}

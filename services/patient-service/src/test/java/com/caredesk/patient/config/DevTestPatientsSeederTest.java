package com.caredesk.patient.config;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.caredesk.patient.model.Patient;
import com.caredesk.patient.repository.PatientRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevTestPatientsSeederTest {

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final DevTestPatientsSeeder seeder = new DevTestPatientsSeeder(patientRepository);

    @Test
    void seedsFifteenTestPatientProfiles() {
        when(patientRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(DevTestPatientsSeeder.TEST_PATIENT_COUNT)).save(patientCaptor.capture());

        assertThat(patientCaptor.getAllValues())
                .hasSize(DevTestPatientsSeeder.TEST_PATIENT_COUNT)
                .allMatch(patient -> patient.getPhoneNumber().startsWith("+49 89 200"))
                .allMatch(patient -> patient.getDateOfBirth() != null);
    }
}

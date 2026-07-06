package com.caredesk.auth.service;

import com.caredesk.auth.model.Role;
import com.caredesk.auth.model.User;
import com.caredesk.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService service = new UserService(userRepository);

    @Test
    void searchDoctorsOmitsPersonalContactDetails() {
        User doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setName("Dr. House");
        doctor.setEmail("house@clinic.com");
        doctor.setRole(Role.DOCTOR);
        doctor.setSpecialization("Diagnostics");
        doctor.setLicenseNumber("LIC-1");
        doctor.setPhoneNumber("+1-555-0100");
        doctor.setDateOfBirth(LocalDate.of(1970, 1, 1));
        when(userRepository.searchDoctors(eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doctor)));

        PaginatedUserProfileResponse response = service.searchDoctors(null, null, 0, 20);

        UserProfile profile = response.getContent().get(0);
        assertThat(profile.getName()).isEqualTo("Dr. House");
        assertThat(profile.getSpecialization()).isEqualTo("Diagnostics");
        assertThat(profile.getPhoneNumber()).isNull();
        assertThat(profile.getDateOfBirth()).isNull();
    }

    @Test
    void searchDoctorsClampsPageSizeAndFloorsNegativePage() {
        when(userRepository.searchDoctors(eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.searchDoctors(null, null, -5, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchDoctors(eq(""), eq(""), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}

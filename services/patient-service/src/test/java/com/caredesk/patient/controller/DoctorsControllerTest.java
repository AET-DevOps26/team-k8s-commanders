package com.caredesk.patient.controller;

import com.caredesk.patient.service.DoctorService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapitools.model.PageMeta;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctorsControllerTest {

    private final DoctorService doctorService = mock(DoctorService.class);
    private final DoctorsController controller = new DoctorsController(doctorService);

    @Test
    void listDoctors_usesDefaultPagination_whenPageAndSizeAreNull() {
        PaginatedUserProfileResponse response = new PaginatedUserProfileResponse(
                List.of(), new PageMeta(0, 20, 0L, 0));
        when(doctorService.listDoctors(isNull(), isNull(), eq(0), eq(20))).thenReturn(response);

        ResponseEntity<PaginatedUserProfileResponse> result = controller.listDoctors(null, null, null, null);

        assertThat(result.getBody()).isSameAs(response);
    }
}

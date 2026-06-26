package com.caredesk.auth.service;

import com.caredesk.auth.model.User;
import org.openapitools.model.UserProfile;
import org.openapitools.model.UserRole;
import org.springframework.stereotype.Component;

/**
 * Maps the internal {@link User} entity to the API-layer {@link UserProfile}
 * model. The internal {@code Role} enum is converted to the generated
 * {@code UserRole} at the boundary. The password is never copied out — it is a
 * write-only field on the API contract.
 */
@Component
public class UserProfileMapper {

    public UserProfile toProfile(User user) {
        UserRole apiRole = UserRole.valueOf(user.getRole().name());
        UserProfile profile = new UserProfile(user.getId(), user.getName(), user.getEmail(), apiRole);
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setDateOfBirth(user.getDateOfBirth());
        profile.setSpecialization(user.getSpecialization());
        profile.setLicenseNumber(user.getLicenseNumber());
        profile.setClinicId(user.getClinicId());
        profile.setEnabled(user.isEnabled());
        return profile;
    }
}

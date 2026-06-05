package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserAccountService;
import java.util.UUID;
import org.openapitools.api.UsersApi;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.openapitools.model.PasswordChangeRequest;
import org.openapitools.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UsersController implements UsersApi {

    private final UserAccountService userAccountService;

    public UsersController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public ResponseEntity<Void> changeUserPassword(UUID userId, PasswordChangeRequest passwordChangeRequest) {
        userAccountService.changePassword(userId, passwordChangeRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserProfile> getUserById(UUID userId) {
        return ResponseEntity.ok(userAccountService.getUser(userId));
    }

    @Override
    public ResponseEntity<PaginatedUserProfileResponse> listUsers(Integer page, Integer size) {
        return ResponseEntity.ok(userAccountService.listUsers(page, size));
    }

    @Override
    public ResponseEntity<UserProfile> replaceUser(UUID userId, UserProfile userProfile) {
        return ResponseEntity.ok(userAccountService.updateUser(userId, userProfile));
    }
}

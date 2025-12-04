package com.was.employeemanagementsystem.controller;

import com.was.employeemanagementsystem.constants.AppConstants;
import com.was.employeemanagementsystem.dto.CreateUserRequest;
import com.was.employeemanagementsystem.dto.CreateUserResponse;
import com.was.employeemanagementsystem.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping(AppConstants.API_USERS_PATH)
@CrossOrigin(origins = "${app.cors.origins}")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Create new user with employee profile
     * SUPER_ADMIN: Can create ADMIN or USER, must select department
     * ADMIN: Can create USER, auto-assigned to their department
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("🔐 User creation request received for: {}", request.getEmail());
        final CreateUserResponse response = userManagementService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


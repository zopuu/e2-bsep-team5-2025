package com.besp.pki.controller;

import com.besp.pki.dto.ApiResponse;
import com.besp.pki.dto.CreateCaUserRequest;
import com.besp.pki.dto.UserDto;
import com.besp.pki.entity.User;
import com.besp.pki.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AdminUsersController {
    private final UserService userService;

    public AdminUsersController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> listUsers(@RequestParam(required = false) String role) {
        var users = userService.listActiveByRoleNullable(role);
        var dto = users.stream().map(u -> new UserDto(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getOrganization(),
                u.getRole().name(),
                u.isEnabled(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getLastLogin()
        )).toList();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/ca")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createCaUser(@Valid @RequestBody CreateCaUserRequest req) {
        try {
            User created = userService.createCaUser(
                    req.getEmail(),
                    req.getFirstName(),
                    req.getLastName(),
                    req.getOrganization()
            );

            return ResponseEntity.ok(ApiResponse.success(
                    "CA user created and invitation email sent.",
                    // lightweight payload back to UI
                    new java.util.HashMap<>() {{
                        put("id", created.getId());
                        put("email", created.getEmail());
                        put("firstName", created.getFirstName());
                        put("lastName", created.getLastName());
                        put("organization", created.getOrganization());
                        put("role", created.getRole().name());
                    }}
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create CA user."));
        }
    }
}

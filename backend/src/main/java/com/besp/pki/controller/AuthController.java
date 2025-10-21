package com.besp.pki.controller;

import com.besp.pki.dto.ApiResponse;
import com.besp.pki.entity.User;
import com.besp.pki.dto.RegistrationRequest;
import com.besp.pki.dto.LoginRequest;
import com.besp.pki.dto.LoginResponse;
import com.besp.pki.security.JwtUtil;
import com.besp.pki.service.PasswordValidationService;
import com.besp.pki.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {
    
    private final UserService userService;
    private final PasswordValidationService passwordValidationService;
    private final JwtUtil jwtUtil;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, PasswordValidationService passwordValidationService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordValidationService = passwordValidationService;
        this.jwtUtil = jwtUtil;
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            log.info("Registration attempt for email={}", request.getEmail());

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                log.warn("Registration failed (passwords do not match) for email={}", request.getEmail());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Passwords do not match"));
            }
            
            // Register user
            userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                request.getOrganization()
            );

            log.info("Registration successful for email={}", request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(
                "Registration successful! Please check your email to activate your account."
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("Registration rejected for email={} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration failed for email={}", request.getEmail(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Registration failed. Please try again."));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for email={}", request.getEmail());

            return userService.findByEmail(request.getEmail())
                .filter(User::isEnabled)
                .map(user -> {
                    if (!userService.passwordMatches(request.getPassword(), user.getPassword())) {
                        log.warn("Login failed (invalid credentials) for email={}", request.getEmail());
                        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials"));
                    }
                    userService.updateLastLogin(user);
                    String role = user.getRole().name();
                    String token = jwtUtil.generateToken(user.getEmail(),role);
                    log.info("User logged in successfully: email={}, role={}", user.getEmail(), role);

                    return ResponseEntity.ok(new LoginResponse(token, "Login successful"));
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials")));
        } catch (Exception e) {
            log.error("Login error for email={}", request.getEmail(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Login failed. Please try again."));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse> activateAccount(@RequestParam String token) {
        try {
            String tokenPrefix = token != null && token.length() > 8 ? token.substring(0, 8) + "..." : "<none>";
            log.info("Activation attempt token={}", tokenPrefix);

            boolean activated = userService.activateUser(token);
            if (activated) {
                log.info("Account activated successfully (tokenPrefix={})", tokenPrefix);
                return ResponseEntity.ok(ApiResponse.success(
                    "Account activated successfully! You can now log in."
                ));
            } else {
                log.warn("Activation failed (invalid/expired token) tokenPrefix={}", tokenPrefix);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired activation token."));
            }
            
        } catch (Exception e) {
            log.error("Activation error", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Activation failed. Please try again."));
        }
    }
    
    @PostMapping("/validate-password")
    public ResponseEntity<ApiResponse> validatePassword(@RequestBody String password) {
        try {
            log.debug("Password validation request received");
            PasswordValidationService.PasswordStrengthResult result = 
                passwordValidationService.validatePassword(password);


            return ResponseEntity.ok(ApiResponse.success(
                result.getMessage(),
                result
            ));
            
        } catch (Exception e) {
            log.error("Password validation failed", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Password validation failed."));
        }
    }
}






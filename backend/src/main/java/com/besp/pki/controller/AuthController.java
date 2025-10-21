package com.besp.pki.controller;

import com.besp.pki.dto.ApiResponse;
import com.besp.pki.entity.User;
import com.besp.pki.dto.RegistrationRequest;
import com.besp.pki.dto.LoginRequest;
import com.besp.pki.dto.LoginResponse;
import com.besp.pki.security.JwtUtil;
import com.besp.pki.service.CaptchaService;
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
    private final CaptchaService captchaService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, PasswordValidationService passwordValidationService, JwtUtil jwtUtil, CaptchaService captchaService) {
        this.userService = userService;
        this.passwordValidationService = passwordValidationService;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            // Validate password confirmation
            if (!request.getPassword().equals(request.getConfirmPassword())) {
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
            
            return ResponseEntity.ok(ApiResponse.success(
                "Registration successful! Please check your email to activate your account."
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration failed", e); // ispisuje ceo stack trace u konzoli
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Registration failed. Please try again."));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Verify captcha first
            String clientIp = null; // optionally extract from request if needed
            boolean captchaOk = captchaService.verify(request.getCaptchaToken(), clientIp);
            if (!captchaOk) {
                return ResponseEntity.badRequest().body(ApiResponse.error("CAPTCHA verification failed"));
            }
            return userService.findByEmail(request.getEmail())
                .filter(User::isEnabled)
                .map(user -> {
                    if (!userService.passwordMatches(request.getPassword(), user.getPassword())) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials"));
                    }
                    userService.updateLastLogin(user);
                    String role = user.getRole().name();
                    String token = jwtUtil.generateToken(user.getEmail(),role,user.getOrganization());
                    return ResponseEntity.ok(new LoginResponse(token, "Login successful"));
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid credentials")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Login failed. Please try again."));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse> activateAccount(@RequestParam String token) {
        try {
            boolean activated = userService.activateUser(token);
            
            if (activated) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Account activated successfully! You can now log in."
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired activation token."));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Activation failed. Please try again."));
        }
    }
    
    @PostMapping("/validate-password")
    public ResponseEntity<ApiResponse> validatePassword(@RequestBody String password) {
        try {
            PasswordValidationService.PasswordStrengthResult result = 
                passwordValidationService.validatePassword(password);
            
            return ResponseEntity.ok(ApiResponse.success(
                result.getMessage(),
                result
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Password validation failed."));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody String email) {
        try {
            userService.createAndSendPasswordResetToken(email);
            
            // Always return success message for security (don't reveal if email exists)
            return ResponseEntity.ok(ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."
            ));
            
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to process password reset request."));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestParam String token, @RequestBody String newPassword) {
        try {
            boolean reset = userService.resetPassword(token, newPassword);
            
            if (reset) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Password reset successfully! You can now log in with your new password."
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired reset token, or password does not meet requirements."));
            }
            
        } catch (Exception e) {
            log.error("Password reset failed", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Password reset failed. Please try again."));
        }
    }
}






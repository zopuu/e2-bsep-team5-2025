package com.besp.pki.controller;

import com.besp.pki.dto.ApiResponse;
import com.besp.pki.dto.PasswordEntryDto;
import com.besp.pki.dto.PasswordEntryRequest;
import com.besp.pki.service.PasswordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passwords")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class PasswordController {
    
    private static final Logger log = LoggerFactory.getLogger(PasswordController.class);
    
    private final PasswordService passwordService;
    
    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<List<PasswordEntryDto>> getPasswords(Authentication auth) {
        try {
            String userEmail = auth.getName();
            log.info("Getting passwords for user: {}", userEmail);
            
            List<PasswordEntryDto> passwords = passwordService.getPasswordsForUser(userEmail);
            
            log.info("Found {} passwords for user", passwords.size());
            return ResponseEntity.ok(passwords);
            
        } catch (Exception e) {
            log.error("Failed to get passwords: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<?> createPassword(@Valid @RequestBody PasswordEntryRequest request, Authentication auth) {
        try {
            String userEmail = auth.getName();
            log.info("Creating password entry for user: {}", userEmail);
            
            PasswordEntryDto password = passwordService.createPassword(request, userEmail);
            
            log.info("Password entry created successfully with ID: {}", password.getId());
            return ResponseEntity.ok(password);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create password entry: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create password entry"));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<ApiResponse> deletePassword(@PathVariable Long id, Authentication auth) {
        try {
            String userEmail = auth.getName();
            log.info("Deleting password entry ID: {} for user: {}", id, userEmail);
            
            passwordService.deletePassword(id, userEmail);
            
            log.info("Password entry deleted successfully");
            return ResponseEntity.ok(ApiResponse.success("Password entry deleted successfully"));
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete password entry: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete password entry"));
        }
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<List<PasswordEntryDto>> searchPasswords(
            @RequestParam String q, 
            Authentication auth) {
        try {
            String userEmail = auth.getName();
            log.info("Searching passwords for user: {}, query: {}", userEmail, q);
            
            List<PasswordEntryDto> passwords = passwordService.searchPasswords(userEmail, q);
            
            log.info("Found {} passwords matching query", passwords.size());
            return ResponseEntity.ok(passwords);
            
        } catch (Exception e) {
            log.error("Failed to search passwords: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

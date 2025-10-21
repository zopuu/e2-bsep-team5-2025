package com.besp.pki.controller;

import com.besp.pki.dto.ActiveSessionDto;
import com.besp.pki.dto.ApiResponse;
import com.besp.pki.security.JwtUtil;
import com.besp.pki.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TokenController {
    
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    
    public TokenController(TokenService tokenService, JwtUtil jwtUtil) {
        this.tokenService = tokenService;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Get all active sessions for the current user
     */
    @GetMapping("/tokens")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<List<ActiveSessionDto>> getActiveSessions(
            HttpServletRequest request,
            Authentication auth) {
        
        String token = extractToken(request);
        List<ActiveSessionDto> sessions = tokenService.getActiveSessions(token);
        
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Revoke a specific token (logout from a specific device)
     */
    @DeleteMapping("/tokens/{jti}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<ApiResponse> revokeToken(
            @PathVariable String jti,
            HttpServletRequest request) {
        
        try {
            String currentToken = extractToken(request);
            tokenService.revokeToken(jti, currentToken);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "Token revoked successfully. You will be logged out from this device."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to revoke token: " + e.getMessage()));
        }
    }
    
    /**
     * Revoke all other tokens (logout from all other devices)
     */
    @DeleteMapping("/tokens/others")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<ApiResponse> revokeAllOtherTokens(HttpServletRequest request) {
        
        try {
            String currentToken = extractToken(request);
            tokenService.revokeAllOtherTokens(currentToken);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "All other sessions have been revoked successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to revoke tokens: " + e.getMessage()));
        }
    }
    
    /**
     * Revoke current token (logout from current device)
     */
    @DeleteMapping("/tokens/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'CA_USER', 'REGULAR_USER')")
    public ResponseEntity<ApiResponse> revokeCurrentToken(HttpServletRequest request) {
        
        try {
            String currentToken = extractToken(request);
            String jti = jwtUtil.getJti(currentToken);
            tokenService.revokeToken(jti, currentToken);
            
            return ResponseEntity.ok(ApiResponse.success(
                    "You have been logged out successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to revoke current token: " + e.getMessage()));
        }
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("No valid token found in request");
    }
}


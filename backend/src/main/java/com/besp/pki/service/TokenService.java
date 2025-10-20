package com.besp.pki.service;

import com.besp.pki.dto.ActiveSessionDto;
import com.besp.pki.entity.ActiveSession;
import com.besp.pki.entity.RevokedToken;
import com.besp.pki.entity.User;
import com.besp.pki.repository.ActiveSessionRepository;
import com.besp.pki.repository.RevokedTokenRepository;
import com.besp.pki.repository.UserRepository;
import com.besp.pki.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TokenService {
    
    private final RevokedTokenRepository revokedTokenRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    public TokenService(RevokedTokenRepository revokedTokenRepository,
                       ActiveSessionRepository activeSessionRepository,
                       UserRepository userRepository, 
                       JwtUtil jwtUtil) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.activeSessionRepository = activeSessionRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Create or update an active session
     */
    public void createOrUpdateSession(String currentToken) {
        Claims claims = jwtUtil.getAllClaims(currentToken);
        String jti = claims.getId();
        String subject = claims.getSubject();
        String device = claims.get("device", String.class);
        String ip = claims.get("ip", String.class);
        
        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        ActiveSession session = activeSessionRepository.findByJti(jti)
                .orElse(new ActiveSession(jti, user, device, ip));
        
        session.setLastActivity(LocalDateTime.now());
        activeSessionRepository.save(session);
    }
    
    /**
     * Revoke a specific token by jti
     */
    public void revokeToken(String jti, String currentToken) {
        String subject = jwtUtil.getSubject(currentToken);
        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Get session info before deleting
        ActiveSession session = activeSessionRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        // Add to revoked tokens
        RevokedToken revokedToken = new RevokedToken(
                jti, 
                user, 
                session.getDeviceInfo(), 
                session.getIpAddress()
        );
        revokedTokenRepository.save(revokedToken);
        
        // Remove from active sessions
        activeSessionRepository.delete(session);
    }
    
    /**
     * Revoke all other tokens except the current one
     */
    public void revokeAllOtherTokens(String currentToken) {
        String currentJti = jwtUtil.getJti(currentToken);
        String subject = jwtUtil.getSubject(currentToken);
        
        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Get all other sessions
        List<ActiveSession> otherSessions = activeSessionRepository.findByUser(user)
                .stream()
                .filter(s -> !s.getJti().equals(currentJti))
                .collect(Collectors.toList());
        
        // Add to revoked tokens
        for (ActiveSession session : otherSessions) {
            RevokedToken revokedToken = new RevokedToken(
                    session.getJti(),
                    user,
                    session.getDeviceInfo(),
                    session.getIpAddress()
            );
            revokedTokenRepository.save(revokedToken);
        }
        
        // Delete all other sessions
        activeSessionRepository.deleteAll(otherSessions);
    }
    
    /**
     * Get active sessions for a user
     */
    public List<ActiveSessionDto> getActiveSessions(String currentToken) {
        String currentJti = jwtUtil.getJti(currentToken);
        String subject = jwtUtil.getSubject(currentToken);
        
        User user = userRepository.findByEmail(subject)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<ActiveSession> sessions = activeSessionRepository.findByUserOrderByLastActivityDesc(user);
        
        return sessions.stream()
                .map(session -> new ActiveSessionDto(
                        session.getJti(),
                        session.getDeviceInfo(),
                        session.getIpAddress(),
                        session.getIssuedAt(),
                        session.getLastActivity(),
                        session.getJti().equals(currentJti) // Mark current session
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a token is revoked
     */
    public boolean isTokenRevoked(String jti) {
        return revokedTokenRepository.existsByJti(jti);
    }
}


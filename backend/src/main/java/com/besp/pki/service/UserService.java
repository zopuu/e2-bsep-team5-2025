package com.besp.pki.service;

import com.besp.pki.entity.ActivationToken;
import com.besp.pki.entity.User;
import com.besp.pki.entity.UserRole;
import com.besp.pki.repository.ActivationTokenRepository;
import com.besp.pki.repository.CaUserSecretRepository;
import com.besp.pki.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordValidationService passwordValidationService;
    private final CaUserSecretService caUserSecretService;
    
    public UserService(UserRepository userRepository, 
                      ActivationTokenRepository activationTokenRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      PasswordValidationService passwordValidationService,
                       CaUserSecretService caUserSecretService) {
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordValidationService = passwordValidationService;
        this.caUserSecretService = caUserSecretService;
    }
    
    public User registerUser(String email, String password, String firstName, String lastName, String organization) {
        // Validate password strength
        PasswordValidationService.PasswordStrengthResult passwordResult = passwordValidationService.validatePassword(password);
        if (!passwordResult.isValid()) {
            throw new IllegalArgumentException("Password validation failed: " + passwordResult.getMessage());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setOrganization(organization);
        user.setRole(UserRole.REGULAR_USER);
        user.setEnabled(false); // Must be activated
        user.setEmailVerified(false);
        
        User savedUser = userRepository.save(user);
        
        // Create and send activation token
        createAndSendActivationToken(savedUser);
        
        return savedUser;
    }
    
    public void createAndSendActivationToken(User user) {
        // Invalidate any existing activation tokens for this user
        List<ActivationToken> existingTokens = activationTokenRepository.findActiveTokensByUser(user);
        for (ActivationToken token : existingTokens) {
            token.markAsUsed();
            activationTokenRepository.save(token);
        }
        
        // Create new activation token (valid for 24 hours)
        ActivationToken activationToken = new ActivationToken();
        activationToken.setUser(user);
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        ActivationToken savedToken = activationTokenRepository.save(activationToken);
        
        // Send activation email
        emailService.sendActivationEmail(user.getEmail(), savedToken.getToken(), user.getFirstName());
    }
    
    public boolean activateUser(String token) {
        Optional<ActivationToken> tokenOpt = activationTokenRepository.findValidToken(token, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            return false; // Token not found or expired
        }
        
        ActivationToken activationToken = tokenOpt.get();
        User user = activationToken.getUser();
        
        // Activate user
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Mark token as used
        activationToken.markAsUsed();
        activationTokenRepository.save(activationToken);
        
        return true;
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> findActiveUsers() {
        return userRepository.findActiveUsers();
    }
    
    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findActiveUsersByRole(role);
    }
    
    public void updateLastLogin(User user) {
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    public void createAndSendPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not for security
            return;
        }
        
        User user = userOpt.get();
        
        // Invalidate any existing password reset tokens for this user
        List<ActivationToken> existingTokens = activationTokenRepository.findActiveTokensByUser(user);
        for (ActivationToken token : existingTokens) {
            token.markAsUsed();
            activationTokenRepository.save(token);
        }
        
        // Create new password reset token (valid for 1 hour)
        ActivationToken resetToken = new ActivationToken();
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        ActivationToken savedToken = activationTokenRepository.save(resetToken);
        
        // Send password reset email
        emailService.sendPasswordResetEmail(user.getEmail(), savedToken.getToken(), user.getFirstName());
    }
    
    public boolean resetPassword(String token, String newPassword) {
        // Validate password strength
        PasswordValidationService.PasswordStrengthResult passwordResult = passwordValidationService.validatePassword(newPassword);
        if (!passwordResult.isValid()) {
            return false;
        }
        
        Optional<ActivationToken> tokenOpt = activationTokenRepository.findValidToken(token, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            return false; // Token not found or expired
        }
        
        ActivationToken activationToken = tokenOpt.get();
        User user = activationToken.getUser();
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark token as used
        activationToken.markAsUsed();
        activationTokenRepository.save(activationToken);
        
        return true;
    }
    public User createCaUser(String email, String firstName, String lastName, String organization) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setOrganization(organization);
        user.setRole(UserRole.CA_USER);

        // strong random password, not shared with the user
        String randomStrong = generateStrongRandomPassword(24);
        user.setPassword(passwordEncoder.encode(randomStrong));

        // we trust admin to have verified identity; enable immediately
        user.setEnabled(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

        // provision per-user sealed AES key for protecting keystore passwords (optional but recommended)
        caUserSecretService.ensureFor(saved);

        // force user to set their own password via email link
        createAndSendCaInvite(saved, 24);

        return saved;
    }
    public void createAndSendCaInvite(User user, int expiresInHours) {
        // Invalidate previous tokens (reuse your existing pattern)
        var existingTokens = activationTokenRepository.findActiveTokensByUser(user);
        for (var t : existingTokens) { t.markAsUsed(); activationTokenRepository.save(t); }

        var token = new ActivationToken();
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(expiresInHours));
        var saved = activationTokenRepository.save(token);

        emailService.sendCaUserInviteEmail(user.getEmail(), saved.getToken(), user.getFirstName(), user.getOrganization());
    }

    // simple generator: A-Z a-z 0-9 and common symbols
    private static String generateStrongRandomPassword(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}:,.?";
        SecureRandom sr = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(sr.nextInt(chars.length())));
        }
        return sb.toString();
    }
    public List<User> listActiveByRoleNullable(String role) {
        // If role is provided, use your existing “active by role” repo call.
        if (role != null && !role.isBlank()) {
            try {
                var enumRole = UserRole.valueOf(role.trim());
                return findUsersByRole(enumRole); // already returns ACTIVE users by role
            } catch (IllegalArgumentException iae) {
                // Unknown role string -> return empty list rather than failing
                return List.of();
            }
        }
        // No role filter -> return active users
        return findActiveUsers();
    }
}






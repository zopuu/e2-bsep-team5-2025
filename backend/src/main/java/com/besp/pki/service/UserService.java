package com.besp.pki.service;

import com.besp.pki.entity.ActivationToken;
import com.besp.pki.entity.User;
import com.besp.pki.entity.UserRole;
import com.besp.pki.repository.ActivationTokenRepository;
import com.besp.pki.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    public UserService(UserRepository userRepository, 
                      ActivationTokenRepository activationTokenRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      PasswordValidationService passwordValidationService) {
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordValidationService = passwordValidationService;
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
}






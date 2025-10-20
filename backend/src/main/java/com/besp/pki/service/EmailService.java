package com.besp.pki.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendActivationEmail(String toEmail, String activationToken, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Activate Your PKI Account");
        
        String activationUrl = frontendUrl + "/activate?token=" + activationToken;
        
        String emailBody = String.format("""
            Hello %s,
            
            Welcome to the PKI System!
            
            Please click the link below to activate your account:
            %s
            
            This link will expire in 24 hours and can only be used once.
            
            If you didn't create this account, please ignore this email.
            
            Best regards,
            PKI System Team
            """, firstName, activationUrl);
        
        message.setText(emailBody);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send activation email", e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetToken, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset Your PKI Password");
        
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        
        String emailBody = String.format("""
            Hello %s,
            
            You requested to reset your password for the PKI System.
            
            Please click the link below to reset your password:
            %s
            
            This link will expire in 1 hour and can only be used once.
            
            If you didn't request this password reset, please ignore this email.
            
            Best regards,
            PKI System Team
            """, firstName, resetUrl);
        
        message.setText(emailBody);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}









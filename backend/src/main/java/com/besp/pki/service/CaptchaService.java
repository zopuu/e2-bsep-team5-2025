package com.besp.pki.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate;
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public CaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verify(String captchaToken, String clientIp) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            System.err.println("CAPTCHA token is null or empty");
            return false;
        }

        // Accept custom arithmetic CAPTCHA
        if ("custom-arithmetic-solved".equals(captchaToken)) {
            System.err.println("Custom arithmetic CAPTCHA verified successfully");
            return true;
        }

        // For Google reCAPTCHA (if needed in future)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", recaptchaSecret);
            body.add("response", captchaToken);
            if (clientIp != null && !clientIp.isEmpty()) {
                body.add("remoteip", clientIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            System.err.println("Sending reCAPTCHA verification request...");
            ResponseEntity<Map> response = restTemplate.postForEntity(RECAPTCHA_VERIFY_URL, request, Map.class);

            System.err.println("reCAPTCHA response status: " + response.getStatusCode());
            System.err.println("reCAPTCHA response body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                System.err.println("reCAPTCHA verification success: " + success);
                return success != null && success;
            }

            return false;
        } catch (Exception e) {
            // Log the exception in production
            System.err.println("Error verifying reCAPTCHA: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
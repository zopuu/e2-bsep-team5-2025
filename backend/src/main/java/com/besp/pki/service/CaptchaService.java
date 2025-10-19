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

    @Value("${turnstile.secret}")
    private String turnstileSecret;

    private final RestTemplate restTemplate;
    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public CaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verify(String captchaToken, String clientIp) {
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", turnstileSecret);
            body.add("response", captchaToken);
            body.add("remoteip", clientIp);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(TURNSTILE_VERIFY_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                return success != null && success;
            }

            return false;
        } catch (Exception e) {
            // Log the exception in production
            System.err.println("Error verifying Turnstile captcha: " + e.getMessage());
            return false;
        }
    }
}
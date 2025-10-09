package com.besp.pki.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CaptchaService {

    @Value("${turnstile.secret}")
    private String secret;

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public boolean verify(String token, String remoteIp) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("secret", secret);
            body.put("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                body.put("remoteip", remoteIp);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Map response = restTemplate.postForObject(VERIFY_URL, request, Map.class);
            Object success = response != null ? response.get("success") : null;
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            return false;
        }
    }
}



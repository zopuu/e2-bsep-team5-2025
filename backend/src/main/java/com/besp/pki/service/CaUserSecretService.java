package com.besp.pki.service;

import com.besp.pki.entity.CaUserSecret;
import com.besp.pki.entity.User;
import com.besp.pki.repository.CaUserSecretRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CaUserSecretService {
    private final CaUserSecretRepository repo;
    private final CryptoSealService cryptoSealService;

    public CaUserSecretService(CaUserSecretRepository repo, CryptoSealService cryptoSealService) {
        this.repo = repo;
        this.cryptoSealService = cryptoSealService;
    }

    /**
     * Ensure there is a sealed per-user AES key (256-bit) for this CA user.
     * Returns the sealed key (not plaintext).
     */
    public String ensureFor(User user) {
        return repo.findByUser(user)
                .map(CaUserSecret::getSealedKey)
                .orElseGet(() -> {
                    String sealed = sealNewRandomKey();
                    CaUserSecret saved = repo.save(new CaUserSecret(user, sealed));
                    return saved.getSealedKey();
                });
    }

    private String sealNewRandomKey() {
        byte[] key = new byte[32]; // 256-bit
        new SecureRandom().nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        try {
            return cryptoSealService.seal(b64);
        } catch (Exception e) {
            throw new RuntimeException("Failed to seal CA user key", e);
        }
    }
}

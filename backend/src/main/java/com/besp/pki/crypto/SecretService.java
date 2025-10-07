package com.besp.pki.crypto;

public interface SecretService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}

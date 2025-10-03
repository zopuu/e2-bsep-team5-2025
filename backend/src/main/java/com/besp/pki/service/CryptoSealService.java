package com.besp.pki.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoSealService {
    private static final String MASTER = "change-this-master-secret-to-env-file";   // TODO: move it to the env file

    public String seal(String plaintext) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        SecureRandom sr = SecureRandom.getInstanceStrong();
        sr.nextBytes(salt);
        sr.nextBytes(iv);

        SecretKeySpec key = derive(MASTER.toCharArray(),salt);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(plaintext.getBytes());

        byte[] out = new byte[salt.length + iv.length + ct.length];
        System.arraycopy(salt, 0, out, 0, salt.length);
        System.arraycopy(iv, 0, out, salt.length, iv.length);
        System.arraycopy(ct, 0, out, salt.length + iv.length, ct.length);
        return Base64.getEncoder().encodeToString(out);
    }
    private SecretKeySpec derive(char[] pass, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass, salt, 100_000, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }
    public String unseal(String sealed) throws Exception {
        byte[] all = Base64.getDecoder().decode(sealed);
        byte[] salt = java.util.Arrays.copyOfRange(all, 0, 16);
        byte[] iv   = java.util.Arrays.copyOfRange(all, 16, 28);
        byte[] ct   = java.util.Arrays.copyOfRange(all, 28, all.length);

        SecretKeySpec key = derive(MASTER.toCharArray(), salt);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] pt = c.doFinal(ct);
        return new String(pt);
    }
}

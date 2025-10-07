package com.besp.pki.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.nio.ByteBuffer;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


public class Pbkdf2AesGcmSecretService implements SecretService {
    private static final byte[] MASTER_SALT = "bsep-master-salt-v1".getBytes(StandardCharsets.UTF_8);
    private static final char[] MASTER_PASS = "bsep-master-pass".toCharArray();     // TODO: promeniti ove dve i odvojiti u .env file

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_LEN_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private SecretKey deriveMasterKey(byte[] perMessageSalt){
        try{
            KeySpec spec = new PBEKeySpec(MASTER_PASS, concat(MASTER_SALT,perMessageSalt),PBKDF2_ITERATIONS, KEY_LEN_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch(Exception e) {
            throw new RuntimeException("PBKDF2 derive failed",e);
        }
    }
    @Override
    public String encrypt(String plaintext){
        try{
            byte[] salt = new byte[16]; RNG.nextBytes(salt);
            byte[] iv = new byte[12]; RNG.nextBytes(iv);
            SecretKey key = deriveMasterKey(salt);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(4+salt.length+4+iv.length+ct.length);
            bb.putInt(salt.length).put(salt).putInt(iv.length).put(iv).put(ct);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch(Exception e) {
            throw new RuntimeException("Encrypt failed",e);
        }
    }
    @Override
    public String decrypt(String cyphertext){
        try{
            byte[] all = Base64.getDecoder().decode(cyphertext);
            ByteBuffer bb = ByteBuffer.wrap(all);
            int saltLen = bb.getInt();
            byte[] salt = new byte[saltLen]; bb.get(salt);
            int ivLen = bb.getInt();
            byte[] iv = new byte[ivLen]; bb.get(iv);
            byte[] ct = new byte[bb.remaining()]; bb.get(ct);
            SecretKey key = deriveMasterKey(salt);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS,iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt,StandardCharsets.UTF_8);
        } catch(Exception e) {
            throw new RuntimeException("Decrypt failed",e);
        }
    }



    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}

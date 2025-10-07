package com.besp.pki.x509;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;

public class FingerprintUtil {
    public static String sha256Hex(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder(dig.length*2);
            for (byte b: dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Fingerprint failed", e);
        }
    }
}

// src/main/java/com/besp/pki/x509/PemUtil.java
package com.besp.pki.x509;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class PemUtil {
    public static String toPem(X509Certificate cert) {
        try {
            String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("PEM encode failed", e);
        }
    }
}

package com.besp.pki.x509;

import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;


public final class X509CaUtils {
    private static final Logger log = LoggerFactory.getLogger(X509CaUtils.class);
    private static final SecureRandom RNG = new SecureRandom();

    // ---------- Common helpers ----------

    public record Validity(Instant now, Instant notBefore, Instant notAfter) {}
    public static Validity validityOf(int years) {
        ZonedDateTime zNow = nowUtc();
        Instant nb = zNow.minusSeconds(60).toInstant();
        Instant na = zNow.plusYears(years).toInstant();
        return new Validity(zNow.toInstant(), nb, na);
    }

    public static void checkIssuerValidity(X509Certificate issuerCert, Instant desiredNotAfter) {
        if (issuerCert.getNotAfter().toInstant().isBefore(desiredNotAfter)) {
            throw new IllegalArgumentException("Subject validity exceeds issuer validity");
        }
    }

    public static Integer resolvePathLen(X509Certificate issuerCert, Integer requested) {
        Integer issuerPathLen = readPathLen(issuerCert);
        if (issuerPathLen == null) return requested; // issuer bez ograničenja -> prepusti traženo
        int maxAllowed = issuerPathLen - 1;
        int subjLen = (requested == null) ? maxAllowed : requested;
        if (subjLen > maxAllowed) throw new IllegalArgumentException("pathLenConstraint exceeds issuer allowance");
        return subjLen;
    }

    public static void buildCaExtensions(X509v3CertificateBuilder builder, PublicKey pub, Integer pathLen) throws Exception {
        JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
        BasicConstraints bc = (pathLen == null) ? new BasicConstraints(true) : new BasicConstraints(pathLen);
        builder.addExtension(Extension.basicConstraints, true, bc);
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(pub));
        // AKI kod self-signed koristi isti public key; kod izdavanja iz issuer-a koristi issuerCert (vidi addAiaAndCrl)
    }

    public static void addAiaAndCrl(X509v3CertificateBuilder builder,
                              X509Certificate issuerCert,
                              String ocspUrl,
                              String crlDp,
                              JcaX509ExtensionUtils extUtil) throws Exception {
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtil.createAuthorityKeyIdentifier(issuerCert));
        if (crlDp != null && !crlDp.isBlank()) {
            DistributionPointName dpn = new DistributionPointName(new GeneralNames(
                    new GeneralName(GeneralName.uniformResourceIdentifier, crlDp)));
            builder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[]{ new DistributionPoint(dpn, null, null) }));
        }
        if (ocspUrl != null && !ocspUrl.isBlank()) {
            AccessDescription ad = new AccessDescription(AccessDescription.id_ad_ocsp,
                    new GeneralName(GeneralName.uniformResourceIdentifier, ocspUrl));
            builder.addExtension(Extension.authorityInfoAccess, false, new AuthorityInformationAccess(ad));
        }
    }

    public static ContentSigner makeContentSigner(String sigAlg, PrivateKey key) throws Exception {
        return new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(key);
    }

    public static boolean notBlank(String s){ return s != null && !s.isBlank(); }
    public static String  escape (String v){ return v.replace(",", "\\,"); }

    public static Integer extractKeySize(PublicKey pk) {
        try {
            if ("RSA".equalsIgnoreCase(pk.getAlgorithm())) {
                return ((java.security.interfaces.RSAPublicKey) pk).getModulus().bitLength();
            } else if ("EC".equalsIgnoreCase(pk.getAlgorithm())) {
                return 256; // ako uvedeš EC, ovde izvuci stvarnu veličinu krive
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Integer readPathLen(X509Certificate cert) {
        try {
            byte[] ext = cert.getExtensionValue(Extension.basicConstraints.getId());
            if (ext == null) return null;
            var der = JcaX509ExtensionUtils.parseExtensionValue(ext);
            BasicConstraints bc = BasicConstraints.getInstance(der);
            if (!bc.isCA()) return null;
            return (bc.getPathLenConstraint()==null) ? null : bc.getPathLenConstraint().intValueExact();
        } catch (Exception e) {
            log.warn("Failed to read pathLen: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isSelfSigned(X509Certificate c) {
        try { c.verify(c.getPublicKey()); return true; } catch (Exception e) { return false; }
    }
    public static boolean isSignedBy(X509Certificate cert, X509Certificate issuer) {
        try { cert.verify(issuer.getPublicKey()); return true; } catch (Exception e) { return false; }
    }

    public static X509Certificate[] assemblePkcs12Chain(
            X509Certificate subjectCert,
            X509Certificate issuerCert,
            X509Certificate[] issuerChainRaw
    ) {
        List<X509Certificate> chain = new ArrayList<>();
        if (issuerChainRaw != null && issuerChainRaw.length > 0) {
            if (!isSignedBy(issuerCert, issuerChainRaw[0])) {
                Collections.reverse(Arrays.asList(issuerChainRaw));
            }
            chain.addAll(Arrays.asList(issuerChainRaw));
        }
        if (chain.isEmpty() || !chain.get(0).equals(issuerCert)) chain.add(0, issuerCert);

        for (int i = 0; i < chain.size() - 1; i++) {
            if (!isSignedBy(chain.get(i), chain.get(i + 1))) {
                throw new IllegalStateException("Issuer chain order invalid at index " + i);
            }
        }
        if (!isSelfSigned(chain.get(chain.size() - 1))) {
            throw new IllegalStateException("Chain does not end with a self-signed root.");
        }

        X509Certificate[] out = new X509Certificate[chain.size() + 1];
        out[0] = subjectCert;
        for (int i = 0; i < chain.size(); i++) out[i + 1] = chain.get(i);

        for (int i = 0; i < out.length - 1; i++) {
            if (!isSignedBy(out[i], out[i + 1])) {
                throw new IllegalStateException("Assembled PKCS12 chain invalid at index " + i);
            }
        }
        if (!isSelfSigned(out[out.length - 1])) {
            throw new IllegalStateException("Assembled PKCS12 chain does not end with self-signed root.");
        }

        try {
            var cf = java.security.cert.CertificateFactory.getInstance("X.509");
            CertPath cp = cf.generateCertPath(Arrays.asList(out));
            var trust = new java.security.cert.TrustAnchor(out[out.length - 1], null);
            var params = new PKIXParameters(Set.of(trust));
            params.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(cp, params);
        } catch (Exception e) {
            throw new IllegalStateException("PKIX validation failed for assembled chain", e);
        }
        return out;
    }

    public static void debugChainForPkcs12(X509Certificate[] chain) {
        if (!log.isDebugEnabled()) return;
        log.debug("---- PKCS12 chain debug (0..n, 0=leaf) ----");
        for (int i = 0; i < chain.length; i++) {
            X509Certificate c = chain[i];
            log.debug("[{}] SUBJ={} \n    ISSR={}", i, c.getSubjectX500Principal(), c.getIssuerX500Principal());
        }
    }

    // ---------- small utils ----------

    public static BigInteger newSerial(int bits) {
        BigInteger s;
        do { s = new BigInteger(bits, RNG).abs(); } while (BigInteger.ZERO.equals(s));
        return s;
    }
    public static ZonedDateTime nowUtc() { return ZonedDateTime.now(ZoneOffset.UTC); }

    public static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
    public static String randomStrong(int len) {
        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+";
        StringBuilder sb = new StringBuilder(len);
        for (int i=0;i<len;i++) sb.append(alphabet.charAt(RNG.nextInt(alphabet.length())));
        return sb.toString();
    }
}

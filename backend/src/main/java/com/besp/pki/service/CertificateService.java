package com.besp.pki.service;

import com.besp.pki.dto.RootCaRequest;
import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.repository.CertificateRecordRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
public class CertificateService {

    private final com.besp.pki.repository.CertificateRecordRepository repo;
    private final CryptoSealService seal;

    @Value("${pki.keystore-dir:./data/keystores}")
    private String keystoreDir;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CertificateService(CertificateRecordRepository repo, CryptoSealService seal) {
        this.repo = repo;
        this.seal = seal;
    }
    public List<CertificateRecord> findAll() {
        return repo.findAll();
    }
    public CertificateRecord createRootCa(RootCaRequest req, String adminEmail) throws Exception {
        // 1) KeyPair (RSA 3072)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(3072);
        KeyPair kp = kpg.generateKeyPair();

        // 2) Subject/Issuer DN (self-signed)
        String dn = buildDn(req);
        X500Name x500 = new X500Name(dn);

        // 3) Serial number + validity
        SecureRandom rnd = SecureRandom.getInstanceStrong();
        BigInteger serial = new BigInteger(64, rnd).abs();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Date notBefore = Date.from(now.toInstant());
        Date notAfter = Date.from(now.plusYears(req.yearsValid).toInstant());

        // 4)X509v3 builder
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                x500, serial, notBefore, notAfter, x500, kp.getPublic()
        );

        // 5) Extensions
        // basicConstaints ( CA = true, pathLen=1)
        builder.addExtension(Extension.basicConstraints,true, new BasicConstraints(1000));
        // stavljeno na hiljhadu da bi podrzao vise intermediate u jednom lancu. ako se ne navede onda sistem
        // moze da sadrzi proizvoljan broj intermediate u jednom lancu

        int usage = KeyUsage.keyCertSign | KeyUsage.cRLSign;
        builder.addExtension(Extension.keyUsage,true, new KeyUsage(usage));

        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded());
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(kp.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(kp.getPublic()));

        // 6) Signature
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(kp.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);

        cert.verify(kp.getPublic());        //sanity check

        // 7) Record in PKCS12 (key + cert) with random pass
        java.nio.file.Files.createDirectories(java.nio.file.Path.of(keystoreDir));
        String alias = "root-" + java.util.UUID.randomUUID();
        String ksPass = randomPass();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, kp.getPrivate(), ksPass.toCharArray(), new java.security.cert.Certificate[]{cert});

        String fileName = alias + ".p12";
        java.nio.file.Path ksPath = java.nio.file.Path.of(keystoreDir, fileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(ksPath.toFile())) {
            ks.store(fos, ksPass.toCharArray());
        }

        // 8) Fingerprint (SHA-256)
        String fingerprint = toHex(java.security.MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));

        // 9) Fill i record CertificateRecord
        CertificateRecord rec = new CertificateRecord();
        rec.setSerialNumber(cert.getSerialNumber().toString(16));
        rec.setType(CertificateType.ROOT);
        rec.setSubjectDn(cert.getSubjectX500Principal().getName());
        rec.setIssuerDn(cert.getIssuerX500Principal().getName());
        rec.setNotBefore(cert.getNotBefore().toInstant());
        rec.setNotAfter(cert.getNotAfter().toInstant());
        rec.setFingerprintSha256(fingerprint);
        rec.setSignatureAlgorithm(cert.getSigAlgName());
        rec.setPublicKeyAlgorithm(cert.getPublicKey().getAlgorithm());
        rec.setKeySize(extractKeySize(cert.getPublicKey()));
        rec.setCa(true);
        rec.setPathLenConstraint(1);
        rec.setKeystorePath(ksPath.toString());
        rec.setKeystoreAlias(alias);
        rec.setEncKeystorePass(seal.seal(ksPass));
        rec.setStatus(CertificateStatus.ACTIVE);
        rec.setCreatedBy(adminEmail);

        return repo.save(rec);
    }
    private String buildDn(RootCaRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("CN=").append(escape(r.commonName));
        if (notBlank(r.organization))        sb.append(", O=").append(escape(r.organization));
        if (notBlank(r.organizationalUnit))  sb.append(", OU=").append(escape(r.organizationalUnit));
        if (notBlank(r.locality))            sb.append(", L=").append(escape(r.locality));
        if (notBlank(r.state))               sb.append(", ST=").append(escape(r.state));
        if (notBlank(r.country))             sb.append(", C=").append(escape(r.country));
        return sb.toString();
    }
    private boolean notBlank(String s){ return s != null && !s.isBlank(); }
    private String escape(String v){ return v.replace(",", "\\,"); }
    private Integer extractKeySize(java.security.PublicKey pk) {
        try {
            if ("RSA".equalsIgnoreCase(pk.getAlgorithm())) {
                return ((java.security.interfaces.RSAPublicKey) pk).getModulus().bitLength();
            } else if ("EC".equalsIgnoreCase(pk.getAlgorithm())) {
                return 256;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private String randomPass() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    public String exportCertificatePem(Long id) throws Exception {
        CertificateRecord rec = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + id));

        // Ako je EE i imamo PEM u bazi
        if (rec.getCertificatePem() != null && !rec.getCertificatePem().isBlank()) {
            return rec.getCertificatePem();
        }

        // Inače učitaj iz PKCS#12 (ROOT/INTERMEDIATE)
        if (rec.getKeystorePath() == null || rec.getKeystoreAlias() == null || rec.getEncKeystorePass() == null)
            throw new IllegalStateException("Certificate is not backed by a keystore");

        String ksPass = seal.unseal(rec.getEncKeystorePass());
        Path ksPath = Path.of(rec.getKeystorePath());
        if (!Files.exists(ksPath)) {
            throw new IllegalStateException("Keystore file missing: " + ksPath);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(ksPath)) {
            ks.load(in, ksPass.toCharArray());
        }
        X509Certificate cert = (X509Certificate) ks.getCertificate(rec.getKeystoreAlias());
        if (cert == null) throw new IllegalStateException("Alias not found in keystore: " + rec.getKeystoreAlias());

        return toPem(cert);
    }

    private String toPem(X509Certificate cert) throws Exception {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
        }
        return sw.toString();
    }


}

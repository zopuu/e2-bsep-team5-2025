package com.besp.pki.service;

import static com.besp.pki.x509.X509CaUtils.*;

import com.besp.pki.crl.CrlService;
import com.besp.pki.dto.*;
import com.besp.pki.entity.CertificateEnums.RevocationReason;
import com.besp.pki.mapper.CertificateMappers;
import com.besp.pki.ocsp.OcspService;
import  com.besp.pki.x509.X500Util;
import static com.besp.pki.x509.PemUtil.*;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.entity.User;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.x509.FingerprintUtil;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;


@Service
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private static final String SIG_ALG = "SHA256withRSA";
    private static final String KS_TYPE = "PKCS12";
    private static final String KEY_ALG_RSA = "RSA";

    private final CertificateRecordRepository repo;
    private final UserService userService;
    private final CryptoSealService seal;
    private final KeyStoreService ks;
    private final CrlService crlService;
    private final OcspService ocspService;

    @Value("${pki.keystore-dir:./data/keystores}")
    private String keystoreDir;

    private static final SecureRandom RNG = new SecureRandom();

    public CertificateService(CertificateRecordRepository repo, CryptoSealService seal, KeyStoreService ks,
                              UserService userService,
                              CrlService crlService,
                              OcspService ocspService) {
        this.repo = repo; this.seal = seal; this.ks = ks ; this.userService = userService;
        this.crlService = crlService; this.ocspService = ocspService;
    }

    public List<CertificateRecord> findAll() { return repo.findAll(); }

    // ---------- ROOT CA ----------

    public CertificateRecord createRootCa(RootCaRequest req, String adminEmail) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALG_RSA);
        kpg.initialize(3072, RNG);
        KeyPair kp = kpg.generateKeyPair();

        String dn = buildDn(req);
        X500Name x500 = new X500Name(dn);

        ZonedDateTime now = nowUtc();
        Date notBefore = Date.from(now.toInstant());
        Date notAfter  = Date.from(now.plusYears(req.yearsValid).toInstant());

        BigInteger serial = newSerial(64);

        X509v3CertificateBuilder builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                x500, serial, notBefore, notAfter, x500, kp.getPublic());

        buildCaExtensions(builder, kp.getPublic(), /*pathLen*/1000);
        ContentSigner signer = makeContentSigner(SIG_ALG, kp.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));

        cert.verify(kp.getPublic()); // sanity

        Files.createDirectories(Path.of(keystoreDir));
        String alias = "root-" + UUID.randomUUID();
        String ksPass = randomStrong(24);

        KeyStore store = KeyStore.getInstance(KS_TYPE);
        store.load(null, null);
        store.setKeyEntry(alias, kp.getPrivate(), ksPass.toCharArray(), new java.security.cert.Certificate[]{cert});

        Path ksPath = Path.of(keystoreDir, alias + ".p12");
        try (var fos = Files.newOutputStream(ksPath)) { store.store(fos, ksPass.toCharArray()); }

        String fingerprint = toHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));

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
        rec.setPathLenConstraint(1000);
        rec.setKeystorePath(ksPath.toString());
        rec.setKeystoreAlias(alias);
        rec.setEncKeystorePass(seal.seal(ksPass));
        rec.setStatus(CertificateStatus.ACTIVE);
        rec.setCreatedBy(adminEmail);

        return repo.save(rec);
    }

    // ---------- INTERMEDIATE CA ----------

    @Transactional
    public CertificateResponse issueIntermediateCA(IntermediateCaRequest req, String createdBy) {
        CertificateRecord issuerRec = repo.findById(req.issuerRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found: " + req.issuerRecordId()));

        if (issuerRec.getStatus() != CertificateStatus.ACTIVE) throw new IllegalStateException("Issuer is not ACTIVE");
        if (!issuerRec.isCa()) throw new IllegalStateException("Issuer is not a CA");

        try {
            char[] issuerPass = seal.unseal(issuerRec.getEncKeystorePass()).toCharArray();
            X509Certificate issuerCert = ks.readCertificate(issuerRec.getKeystorePath(), issuerRec.getKeystoreAlias(), issuerPass);
            X509Certificate[] issuerChain = ks.readChain(issuerRec.getKeystorePath(), issuerRec.getKeystoreAlias(), issuerPass);
            for (int i=0;i<issuerChain.length;i++) {
                log.debug("issuerChainRaw[{}] SUBJ={} ISSR={}", i,
                        issuerChain[i].getSubjectX500Principal(),
                        issuerChain[i].getIssuerX500Principal());
            }
            PrivateKey issuerKey = ks.readPrivateKey(issuerRec.getKeystorePath(), issuerRec.getKeystoreAlias(), issuerPass);

            var validity = validityOf(req.yearsValid());
            checkIssuerValidity(issuerCert, validity.notAfter());

            if (req.pathLenConstraint() != null && req.pathLenConstraint() < 0) {
                throw new IllegalArgumentException("pathLenConstraint must be >= 0");
            }
            Integer issuerPathLen = readPathLen(issuerCert);
            if (issuerPathLen != null) {
                if (issuerPathLen <= 0) {
                    throw new IllegalArgumentException("Issuer has pathLenConstraint=0 and cannot issue a CA.");
                }
            }
            Integer requestedPathLen = resolvePathLen(issuerCert, req.pathLenConstraint());
            if (requestedPathLen != null && requestedPathLen < 0) {
                throw new IllegalArgumentException("pathLenConstraint cannot be negative");
            }

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALG_RSA);
            kpg.initialize(4096, RNG);
            KeyPair subjectKP = kpg.generateKeyPair();

            X500Name issuerDN  = X500Name.getInstance(issuerCert.getSubjectX500Principal().getEncoded());
            X500Name subjectDN = X500Util.fromDto(req.subject());

            BigInteger serial = newSerial(160);
            String serialHex = serial.toString(16);

            JcaX509ExtensionUtils extUtil = new JcaX509ExtensionUtils();
            X509v3CertificateBuilder builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                    issuerDN, serial, Date.from(validity.notBefore()), Date.from(validity.notAfter()),
                    subjectDN, subjectKP.getPublic());

            buildCaExtensions(builder, subjectKP.getPublic(), requestedPathLen);
            addAiaAndCrl(builder, issuerCert, req.ocspUrl(), req.crlDistributionPoint(), extUtil);

            ContentSigner signer = makeContentSigner(SIG_ALG, issuerKey);
            X509Certificate subjectCert = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(builder.build(signer));

            subjectCert.checkValidity(Date.from(validity.now()));
            subjectCert.verify(issuerCert.getPublicKey());

            X509Certificate[] newChain = assemblePkcs12Chain(subjectCert, issuerCert, issuerChain);
            debugChainForPkcs12(newChain);

            // provera public key u leaf-u
            if (!Arrays.equals(subjectCert.getPublicKey().getEncoded(), subjectKP.getPublic().getEncoded())) {
                throw new IllegalStateException("Leaf cert public key doesn't match generated subject keypair!");
            }

            String alias = "ca-" + serialHex;
            String entryPassPlain = randomStrong(24);
            var ref = ks.storeEntryPerCert(keystoreDir, alias, subjectKP.getPrivate(), entryPassPlain.toCharArray(), newChain);

            List<String> chainSubjectDns = Arrays.stream(newChain).map(c -> c.getSubjectX500Principal().getName()).toList();

            CertificateRecord rec = new CertificateRecord();
            rec.setSerialNumber(serialHex);
            rec.setType(CertificateType.INTERMEDIATE);
            rec.setSubjectDn(subjectCert.getSubjectX500Principal().getName());
            rec.setIssuerDn(issuerCert.getSubjectX500Principal().getName());
            rec.setNotBefore(validity.notBefore());
            rec.setNotAfter(validity.notAfter());
            rec.setFingerprintSha256(FingerprintUtil.sha256Hex(subjectCert));
            rec.setSignatureAlgorithm(SIG_ALG);
            rec.setPublicKeyAlgorithm(KEY_ALG_RSA);
            rec.setKeySize(4096);
            rec.setCa(true);
            rec.setPathLenConstraint(requestedPathLen);
            rec.setStatus(CertificateStatus.ACTIVE);
            rec.setKeystorePath(ref.path());
            rec.setKeystoreAlias(ref.alias());
            rec.setEncKeystorePass(seal.seal(entryPassPlain));
            rec.setCertificatePem(toPem(subjectCert));
            rec.setCrlDistrigutionPoint(req.crlDistributionPoint());
            rec.setOcspUrl(req.ocspUrl());
            rec.setIssuer(issuerRec);
            if (req.ownerUserId() != null) { User u = new User(); u.setId(req.ownerUserId()); rec.setOwner(u); }
            rec.setCreatedBy(createdBy);
            repo.save(rec);

            return new CertificateResponse(
                    rec.getSerialNumber(), rec.getSubjectDn(), rec.getIssuerDn(),
                    rec.getNotBefore(), rec.getNotAfter(), rec.isCa(), rec.getPathLenConstraint(), chainSubjectDns
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Intermediate CA issuance failed", e);
        }
    }
    public String exportCertificatePem(Long id) throws Exception {
        CertificateRecord rec = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + id));

        if (rec.getCertificatePem() != null && !rec.getCertificatePem().isBlank()) return rec.getCertificatePem();

        if (rec.getKeystorePath() == null || rec.getKeystoreAlias() == null || rec.getEncKeystorePass() == null)
            throw new IllegalStateException("Certificate is not backed by a keystore");

        String ksPass = seal.unseal(rec.getEncKeystorePass());
        Path ksPath = Path.of(rec.getKeystorePath());
        if (!Files.exists(ksPath)) throw new IllegalStateException("Keystore file missing: " + ksPath);

        KeyStore store = KeyStore.getInstance(KS_TYPE);
        try (var in = Files.newInputStream(ksPath)) { store.load(in, ksPass.toCharArray()); }
        X509Certificate cert = (X509Certificate) store.getCertificate(rec.getKeystoreAlias());
        if (cert == null) throw new IllegalStateException("Alias not found in keystore: " + rec.getKeystoreAlias());
        return toPem(cert);
    }
    private static String buildDn(RootCaRequest r) {
        StringBuilder sb = new StringBuilder();
        sb.append("CN=").append(escape(r.commonName));
        if (notBlank(r.organization))        sb.append(", O=").append(escape(r.organization));
        if (notBlank(r.organizationalUnit))  sb.append(", OU=").append(escape(r.organizationalUnit));
        if (notBlank(r.locality))            sb.append(", L=").append(escape(r.locality));
        if (notBlank(r.state))               sb.append(", ST=").append(escape(r.state));
        if (notBlank(r.country))             sb.append(", C=").append(escape(r.country));
        return sb.toString();
    }
    public PagedResponse<CertificateListItem> search(
            String q,
            CertificateType type,
            CertificateStatus status,
            Boolean ca,
            int page, int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<CertificateRecord> spec = Specification.where(null);

        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("serialNumber")), like),
                    cb.like(cb.lower(root.get("subjectDn")), like),
                    cb.like(cb.lower(root.get("issuerDn")), like)
            ));
        }
        if (type != null)   spec = spec.and((r, qy, cb) -> cb.equal(r.get("type"), type));
        if (status != null) spec = spec.and((r, qy, cb) -> cb.equal(r.get("status"), status));
        if (ca != null)     spec = spec.and((r, qy, cb) -> cb.equal(r.get("ca"), ca));

        Page<CertificateRecord> p = repo.findAll(spec, pageable);

        return new PagedResponse<>(
                p.map(CertificateMappers::toListItem).getContent(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumber(),
                p.getSize()
        );
    }
    public CertificateChainDto readChain(long id) throws Exception {
        var rec = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + id));

        List<String> subjects;

        if (rec.getKeystorePath() != null && rec.getKeystoreAlias() != null && rec.getEncKeystorePass() != null) {
            char[] pass = seal.unseal(rec.getEncKeystorePass()).toCharArray();
            X509Certificate[] chain = ks.readChain(rec.getKeystorePath(), rec.getKeystoreAlias(), pass);
            subjects = Arrays.stream(chain)
                    .map(c -> c.getSubjectX500Principal().getName())
                    .toList();
        } else {
            // fallback: at least return the leaf subject
            subjects = List.of(rec.getSubjectDn());
        }

        return new CertificateChainDto(rec.getId(), rec.getSubjectDn(), rec.getIssuerDn(), subjects);
    }
    @Transactional
    public void revoke(long id, RevocationReason reason, String revokedBy) {
        var rec = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + id));

        if (rec.getStatus() == CertificateStatus.REVOKED) {
            return; // idempotent
        }

        rec.setStatus(CertificateStatus.REVOKED);
        rec.setRevocationReason(reason);
        rec.setRevocationDate(Instant.now());
        rec.setUpdatedAt(Instant.now());
        // optional audit breadcrumb
        String by = (revokedBy == null ? "admin" : revokedBy);
        rec.setCreatedBy((rec.getCreatedBy() == null ? "" : rec.getCreatedBy()) + " | revoked by " + by);

        Long issuerId = rec.getIssuer() != null ? rec.getIssuer().getId() : rec.getId(); // root je sam sebi issuer
        try {
            crlService.rebuild(issuerId);
        } catch (Exception e) { /* log.warn("CRL rebuild failed", e); */ }
        try {
            ocspService.invalidate(issuerId, rec.getSerialNumber());
        } catch (Exception e) { /* log.warn("OCSP invalidate failed", e); */ }

        repo.save(rec);
    }
    public List<CaIssuerDto> findActiveIssuersForCaUserEmail(String email) {
        var userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) return List.of();
        var user = userOpt.get();
        var now = Instant.now();

        return repo.findActiveCaIssuerDtosByOwnerUserId(user.getId(), CertificateStatus.ACTIVE, now);
    }
    /**
     * CA user issues an Intermediate CA from an issuer in *their* chain/scope.
     * - Verifies issuer is ACTIVE CA and currently valid.
     * - Verifies SCOPE: issuer owned by this CA user OR same organization.
     * - Forces owner to be the CA user (ignores any ownerUserId sent by client).
     * - Reuses the existing admin implementation for actual issuance.
     */
    public CertificateResponse issueIntermediateCAForCaUser(IntermediateCaRequest req, String caUserEmail) {
        User caUser = userService.findByEmail(caUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("Unknown CA user"));

        // === issuer scope check (owner-based) - unchanged ===
        CertificateRecord issuer = repo.findById(req.issuerRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found: " + req.issuerRecordId()));
        if (!issuer.isCa() || issuer.getStatus() != CertificateStatus.ACTIVE) {
            throw new IllegalArgumentException("Issuer is not an active CA");
        }
        var now = Instant.now();
        if (issuer.getNotBefore().isAfter(now) || issuer.getNotAfter().isBefore(now)) {
            throw new IllegalArgumentException("Issuer is not currently valid");
        }
        boolean okOwner = issuer.getOwner() != null && issuer.getOwner().getId() != null
                && issuer.getOwner().getId().equals(caUser.getId());
        boolean okOrg = issuer.getOwner() != null && issuer.getOwner().getOrganization() != null
                && issuer.getOwner().getOrganization().equals(caUser.getOrganization());
        if (!okOwner && !okOrg) {
            throw new IllegalArgumentException("Issuer is outside your CA scope");
        }

        // === NEW: force/validate organization on the subject ===
        String userOrg = Optional.ofNullable(caUser.getOrganization()).map(String::trim).orElse("");
        if (userOrg.isEmpty()) {
            throw new IllegalStateException("Your account is missing organization. Contact an admin.");
        }
        String reqOrg = Optional.ofNullable(req.subject().organization()).map(String::trim).orElse("");

        NameDto enforcedSubject;
        if (reqOrg.isEmpty()) {
            // Auto-fill O to the CA user’s org
            enforcedSubject = new NameDto(
                    req.subject().commonName(),
                    userOrg,
                    req.subject().organizationalUnit(),
                    req.subject().country(),
                    req.subject().state(),
                    req.subject().locality()
            );
        } else if (!reqOrg.equals(userOrg)) {
            throw new IllegalArgumentException("Subject.O must be '" + userOrg + "' for your organization.");
        } else {
            enforcedSubject = req.subject(); // matches, keep as-is
        }

        // Force owner to this CA user and reuse core logic
        var caScopedReq = new IntermediateCaRequest(
                req.issuerRecordId(),
                enforcedSubject,
                req.yearsValid(),
                req.pathLenConstraint(),
                req.crlDistribuitionPoints(),
                req.authorityInfoAccessOcsp(),
                req.crlDistributionPoint(),
                req.ocspUrl(),
                caUser.getId()
        );

        return this.issueIntermediateCA(caScopedReq, caUserEmail);
    }



}

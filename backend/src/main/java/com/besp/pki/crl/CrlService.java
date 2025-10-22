package com.besp.pki.crl;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.RevocationReason;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.service.CryptoSealService;
import com.besp.pki.service.KeyStoreService;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CrlService {

    private final CertificateRecordRepository certRepo;
    private final CrlCounterRepository counterRepo;
    private final KeyStoreService ks;
    private final CryptoSealService seal;

    // keš u memoriji: issuerId → DER bytes
    private final Map<Long, byte[]> cache = new HashMap<>();

    public byte[] currentCrl(long issuerId) {
        return cache.get(issuerId);
    }

    @Transactional
    public byte[] rebuild(long issuerId) {
        CertificateRecord issuer = certRepo.findById(issuerId)
                .orElseThrow(() -> new IllegalArgumentException("Issuer not found: " + issuerId));

        List<CertificateRecord> revoked = certRepo.findByIssuer_IdAndStatus(issuerId, CertificateStatus.REVOKED);

        try {
            char[] pass = seal.unseal(issuer.getEncKeystorePass()).toCharArray();
            X509Certificate issuerCert = ks.readCertificate(issuer.getKeystorePath(), issuer.getKeystoreAlias(), pass);
            PrivateKey issuerKey = ks.readPrivateKey(issuer.getKeystorePath(), issuer.getKeystoreAlias(), pass);

            Date thisUpdate = new Date();
            Date nextUpdate = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

            var builder = new JcaX509v2CRLBuilder(issuerCert, thisUpdate);
            builder.setNextUpdate(nextUpdate);

            // CRL Number (+ AuthorityKeyIdentifier nije obavezno ali korisno)
            long number = nextNumber(issuerId);
            builder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(number)));
            var extUtils = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(issuerCert));

            for (CertificateRecord r : revoked) {
                if (r.getRevocationDate() == null) continue;
                BigInteger serial = new BigInteger(r.getSerialNumber(), 16);
                int reasonCode = mapReason(r.getRevocationReason());
                builder.addCRLEntry(serial, Date.from(r.getRevocationDate()), reasonCode);
            }

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey);
            X509CRLHolder holder = builder.build(signer);
            byte[] der = holder.getEncoded();

            cache.put(issuerId, der);
            bumpNumber(issuerId, number);

            return der;
        } catch (Exception e) {
            throw new RuntimeException("CRL build failed for issuer " + issuerId, e);
        }
    }

    private long nextNumber(long issuerId) {
        var cc = counterRepo.findByIssuerId(issuerId).orElseGet(() -> {
            var x = new CrlCounter(); x.setIssuerId(issuerId); x.setCrlNumber(0); return x;
        });
        return cc.getCrlNumber() + 1;
    }

    private void bumpNumber(long issuerId, long num) {
        var cc = counterRepo.findByIssuerId(issuerId).orElseGet(() -> {
            var x = new CrlCounter(); x.setIssuerId(issuerId); return x;
        });
        cc.setCrlNumber(num);
        cc.setLastBuiltAt(Instant.now());
        counterRepo.save(cc);
    }

    private int mapReason(RevocationReason r) {
        if (r == null) return CRLReason.unspecified;
        switch (r) {
            case KEY_COMPROMISED:        return CRLReason.keyCompromise;
            case CA_COMPROMISED:         return CRLReason.cACompromise;
            case AFFILIATION_CHANGED:    return CRLReason.affiliationChanged;
            case SUPERSEDED:             return CRLReason.superseded;
            case CESSATION_OF_OPERATION: return CRLReason.cessationOfOperation;
            case CERTIFICATE_HOLD:       return CRLReason.certificateHold;
            case REMOVE_FROM_CRL:        return CRLReason.removeFromCRL;
            case PRIVILEGE_WITHDRAWN:    return CRLReason.privilegeWithdrawn;
            case AA_COMPROMISED:         return CRLReason.aACompromise;
            case UNSPECIFIED:            return CRLReason.unspecified;
        }
        // unreachable
        return CRLReason.unspecified;
    }

}

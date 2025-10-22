package com.besp.pki.ocsp;

import com.besp.pki.entity.CertificateEnums.RevocationReason;
import com.besp.pki.entity.CertificateRecord;
import com.besp.pki.repository.CertificateRecordRepository;
import com.besp.pki.service.CryptoSealService;
import com.besp.pki.service.KeyStoreService;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OcspService {

    private final CertificateRecordRepository repo;
    private final KeyStoreService ks;
    private final CryptoSealService seal;

    public byte[] handle(byte[] requestBytes) {
        try {
            OCSPReq req = new OCSPReq(requestBytes);
            Req[] list = req.getRequestList();
            if (list == null || list.length == 0) {
                return new OCSPRespBuilder().build(OCSPRespBuilder.MALFORMED_REQUEST, null).getEncoded();
            }

            // Digest provider (jedan po zahtevu je sasvim OK)
            DigestCalculatorProvider dcp = new JcaDigestCalculatorProviderBuilder().build();

            // Izabraćemo jednog responder-a (issuer) za ovaj req
            X509Certificate issuerCert = null;
            PrivateKey signerKey = null;

            // 1) Pokušaj najpre preko 1. stavke — ako serial postoji u DB, znamo issuer-a
            CertificateID firstCid = list[0].getCertID();
            BigInteger firstSerial = firstCid.getSerialNumber();
            Optional<CertificateRecord> firstOpt = repo.findBySerialNumberIgnoreCase(firstSerial.toString(16));

            if (firstOpt.isPresent()) {
                CertificateRecord target = firstOpt.get();
                CertificateRecord issuerRec = (target.getIssuer() != null ? target.getIssuer() : target); // root je sam sebi issuer
                char[] pass = seal.unseal(issuerRec.getEncKeystorePass()).toCharArray();
                issuerCert = ks.readCertificate(issuerRec.getKeystorePath(), issuerRec.getKeystoreAlias(), pass);
                signerKey  = ks.readPrivateKey(issuerRec.getKeystorePath(), issuerRec.getKeystoreAlias(), pass);
            } else {
                // 2) Serial ne postoji → probaj da pronađeš issuer-a koji se poklapa sa CertID (matchesIssuer)
                for (CertificateRecord cand : repo.findAll()) {
                    if (!cand.isCa()) continue;
                    try {
                        char[] pass = seal.unseal(cand.getEncKeystorePass()).toCharArray();
                        X509Certificate candCert = ks.readCertificate(cand.getKeystorePath(), cand.getKeystoreAlias(), pass);
                        if (firstCid.matchesIssuer(new X509CertificateHolder(candCert.getEncoded()), dcp)) {
                            issuerCert = candCert;
                            signerKey  = ks.readPrivateKey(cand.getKeystorePath(), cand.getKeystoreAlias(), pass);
                            break;
                        }
                    } catch (Exception ignore) { /* skip candidate */ }
                }
                // ako i dalje nemamo issuer-a, ne možemo potpisati – unauthorized
                if (issuerCert == null || signerKey == null) {
                    return new OCSPRespBuilder().build(OCSPRespBuilder.UNAUTHORIZED, null).getEncoded();
                }
            }

            // Sada imamo issuerCert + signerKey → možemo da napravimo builder
            var spki = new X509CertificateHolder(issuerCert.getEncoded()).getSubjectPublicKeyInfo();
            BasicOCSPRespBuilder builder = new BasicOCSPRespBuilder(spki, dcp.get(CertificateID.HASH_SHA1));

            // Obradi sve zahteve u ovom OCSPReq (tipično je 1)
            for (Req r : list) {
                CertificateID cid = r.getCertID();
                BigInteger serial = cid.getSerialNumber();
                Optional<CertificateRecord> opt = repo.findBySerialNumberIgnoreCase(serial.toString(16));

                if (opt.isEmpty()) {
                    // ne znamo taj serial → Unknown
                    builder.addResponse(cid, new UnknownStatus(), new Date(), nextUpdate());
                    continue;
                }

                CertificateRecord target = opt.get();
                // CertID u odgovoru mora biti Građen iz NAŠEG issuerCert-a + serijskog target-a
                CertificateID respCid = new CertificateID(
                        dcp.get(CertificateID.HASH_SHA1),
                        new X509CertificateHolder(issuerCert.getEncoded()),
                        new BigInteger(target.getSerialNumber(), 16)
                );

                switch (target.getStatus()) {
                    case REVOKED:
                        builder.addResponse(
                                respCid,
                                new RevokedStatus(Date.from(target.getRevocationDate()), mapReason(target.getRevocationReason())),
                                new Date(),
                                nextUpdate(),
                                null
                        );
                        break;
                    case ACTIVE:
                    case EXPIRED:
                        builder.addResponse(
                                respCid,
                                org.bouncycastle.cert.ocsp.CertificateStatus.GOOD,
                                new Date(),
                                nextUpdate(),
                                null
                        );
                        break;
                }
            }

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(signerKey);
            X509CertificateHolder[] chain = { new X509CertificateHolder(issuerCert.getEncoded()) };
            BasicOCSPResp basic = builder.build(contentSigner, chain, new Date());
            return new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basic).getEncoded();

        } catch (Exception e) {
            throw new RuntimeException("OCSP failed", e);
        }
    }

    public void invalidate(long issuerId, String serialHex) {
        // Ako budeš keširao odgovore – očisti ih ovde (trenutno nemamo keš).
    }

    private Date nextUpdate() {
        return new Date(System.currentTimeMillis() + 60 * 60 * 1000); // 1h
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
        // defensive default
        return CRLReason.unspecified;
    }
}

package com.besp.pki.entity;

import com.besp.pki.entity.CertificateEnums.CertificateStatus;
import com.besp.pki.entity.CertificateEnums.CertificateType;
import com.besp.pki.entity.CertificateEnums.RevocationReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Entity
@Table(name = "certificates",
        uniqueConstraints = {
            @UniqueConstraint(name= "uk_cert_serial", columnNames = "serial_number"),
            @UniqueConstraint(name = "uk_cert_fingerprint", columnNames = "fingerprint_sha256")
        },
        indexes = {
            @Index(name = "ix_cert_type", columnList = "type"),
            @Index(name = "ix_cert_status", columnList = "status"),
            @Index(name = "ix_cert_owner", columnList = "owner_user_id"),
            @Index(name = "ix_cert_issuer", columnList = "issuer_id")
        })
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // basic fields for X.509
    @Column(name = "serial_number", nullable = false,length = 64)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name= "type", nullable = false,length = 16)
    private CertificateType type;

    @Column(name = "subject_dn", nullable = false,length = 1024)
    private String subjectDn;

    @Column(name = "issuer_dn", nullable = false,length = 1024)
    private String issuerDn;

    @Column(name = "not_before", nullable = false)
    private Instant notBefore;

    @Column(name = "not_after", nullable = false)
    private Instant notAfter;

    // Meta for revision and quick recognition
    @Column(name = "fingerprint_sha256", nullable = false,length = 64)
    private String fingerprintSha256;

    @Column(name = "sign_alg", length = 64)
    private String signatureAlgorithm;

    @Column(name = "pubkey_alg", nullable = false,length = 32)
    private String publicKeyAlgorithm;

    @Column(name = "key_size")
    private Integer keySize;

    // Extensions for displaying/policies

    @Column(name = "is_ca",nullable = false)
    private boolean ca;

    @Column(name = "path_len_constraint")
    private Integer pathLenConstraint;

    // Revocation
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,length = 16)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason",length = 32)
    private RevocationReason revocationReason;


    @Column(name = "revocation_date")
    private Instant revocationDate;

    @Column(name = "keysotre_path",length = 1024)
    private String keysotrePath;        // ex. ./data/keystores/root-uuid.p12

    @Column(name = "keystore_alias", length = 128)
    private String keystoreAlias;

    @Column(name = "enc_keystore_pass", length = 2048)
    private String encKeystorePass;

    @Lob
    @Column(name = "certificate_pem")
    private String certificatePem;

    @Lob
    @Column(name = "sam_json")
    private String subjectAltNamesJson;     // ex. JSON array DNS/IP/URI

    @Column(name = "crl_distrigution_point", length = 1024)
    private String crlDistrigutionPoint;    // URL do CRL-a

    @Column(name = "ocsp_url", length = 1024)
    private String ocspUrl;

    // Relacije za lanac i vlasništvo/prava prikaza
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id")
    private CertificateRecord issuer;   // self-reference → ko je potpisao ovaj cert

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;                 // EE: vlasnik; CA: vlasnik CA sertifikata (CA korisnik)

    // Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 256)
    private String createdBy; // email/admin id

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

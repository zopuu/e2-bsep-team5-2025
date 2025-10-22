package com.besp.pki.crl;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "crl_counters", uniqueConstraints = @UniqueConstraint(columnNames = "issuer_id"))
@Setter
@Getter
public class CrlCounter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="issuer_id", nullable=false)
    private Long issuerId;

    @Column(name="crl_number", nullable=false)
    private long crlNumber;

    @Column(name="last_built_at", nullable=false)
    private Instant lastBuiltAt = Instant.now();

}

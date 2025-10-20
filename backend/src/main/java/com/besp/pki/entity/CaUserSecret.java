package com.besp.pki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ca_user_secrets",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id"})})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CaUserSecret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1-1 to your User
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // base64(aes-256 key) sealed with CryptoSealService (AES-GCM over a PBKDF2 key)
    @Column(name = "sealed_key", nullable = false, length = 2048)
    private String sealedKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

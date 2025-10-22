package com.besp.pki.crl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrlCounterRepository extends JpaRepository<CrlCounter, Long> {
    Optional<CrlCounter> findByIssuerId(Long issuerId);
}

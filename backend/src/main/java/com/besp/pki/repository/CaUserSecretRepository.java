package com.besp.pki.repository;

import com.besp.pki.entity.CaUserSecret;
import com.besp.pki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaUserSecretRepository extends JpaRepository<CaUserSecret, Long> {
    Optional<CaUserSecret> findByUser(User user);
    boolean existsByUser(User user);
}

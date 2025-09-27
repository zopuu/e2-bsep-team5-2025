package com.besp.pki.repository;

import com.besp.pki.entity.ActivationToken;
import com.besp.pki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    
    Optional<ActivationToken> findByToken(String token);
    
    List<ActivationToken> findByUser(User user);
    
    @Query("SELECT at FROM ActivationToken at WHERE at.token = :token AND at.used = false AND at.expiresAt > :now")
    Optional<ActivationToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    @Query("SELECT at FROM ActivationToken at WHERE at.expiresAt < :now")
    List<ActivationToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT at FROM ActivationToken at WHERE at.user = :user AND at.used = false")
    List<ActivationToken> findActiveTokensByUser(@Param("user") User user);
}



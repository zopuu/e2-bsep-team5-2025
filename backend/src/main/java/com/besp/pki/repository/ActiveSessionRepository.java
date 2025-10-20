package com.besp.pki.repository;

import com.besp.pki.entity.ActiveSession;
import com.besp.pki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, String> {
    
    Optional<ActiveSession> findByJti(String jti);
    
    List<ActiveSession> findByUser(User user);
    
    List<ActiveSession> findByUserOrderByLastActivityDesc(User user);
    
    @Modifying
    @Query("DELETE FROM ActiveSession a WHERE a.user = :user AND a.jti != :currentJti")
    void deleteAllOtherSessions(@Param("user") User user, @Param("currentJti") String currentJti);
    
    @Modifying
    @Query("DELETE FROM ActiveSession a WHERE a.lastActivity < :expiryTime")
    void deleteExpiredSessions(@Param("expiryTime") LocalDateTime expiryTime);
}


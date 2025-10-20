package com.besp.pki.repository;

import com.besp.pki.entity.User;
import com.besp.pki.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByOrganization(String organization);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.emailVerified = true")
    List<User> findActiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.enabled = false OR u.emailVerified = false")
    List<User> findInactiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = true AND u.emailVerified = true")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);
}









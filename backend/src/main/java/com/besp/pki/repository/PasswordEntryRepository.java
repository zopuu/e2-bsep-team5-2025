package com.besp.pki.repository;

import com.besp.pki.entity.PasswordEntry;
import com.besp.pki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {
    
    List<PasswordEntry> findByOwnerOrderByCreatedAtDesc(User owner);
    
    List<PasswordEntry> findByOwnerAndSiteNameContainingIgnoreCaseOrderByCreatedAtDesc(User owner, String siteName);
    
    Optional<PasswordEntry> findByIdAndOwner(Long id, User owner);
    
    @Query("SELECT p FROM PasswordEntry p WHERE p.owner = :owner AND " +
           "(LOWER(p.siteName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<PasswordEntry> findByOwnerAndSearchTerm(@Param("owner") User owner, @Param("searchTerm") String searchTerm);
    
    long countByOwner(User owner);
}

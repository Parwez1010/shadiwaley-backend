package com.shadiwaley.server.parent.infrastructure.repository;

import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, UUID> {
    Optional<ParentProfile> findByUserAccountId(UUID userAccountId);
    Optional<ParentProfile> findTopByUserAccountIdOrderByCreatedAtDesc(UUID userAccountId);

    @Query("""
        SELECT DISTINCT p.district
        FROM ParentProfile p
        WHERE p.district IS NOT NULL AND p.district <> ''
        ORDER BY p.district
        """)
    List<String> findDistinctDistricts();

    @Query("""
        SELECT DISTINCT p.state
        FROM ParentProfile p
        WHERE p.state IS NOT NULL AND p.state <> ''
        ORDER BY p.state
        """)
    List<String> findDistinctStates();

    @Query("""
        SELECT DISTINCT p.caste
        FROM ParentProfile p
        WHERE p.caste IS NOT NULL AND p.caste <> ''
        ORDER BY p.caste
        """)
    List<String> findDistinctCastes();

    @Query("""
        SELECT DISTINCT p.maslak
        FROM ParentProfile p
        WHERE p.maslak IS NOT NULL AND p.maslak <> ''
        ORDER BY p.maslak
        """)
    List<String> findDistinctMaslak();


}
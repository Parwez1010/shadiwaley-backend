package com.shadiwaley.server.profile.infrastructure.repository;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, JpaSpecificationExecutor<UserProfile> {
    Optional<UserProfile> findByUserAccountId(UUID userAccountId);
    long countByProfileStatus(com.shadiwaley.server.profile.domain.ProfileStatus profileStatus);

    java.util.List<com.shadiwaley.server.profile.infrastructure.entity.UserProfile>
    findTop20ByProfileStatusOrderByCreatedAtDesc(
            com.shadiwaley.server.profile.domain.ProfileStatus profileStatus
    );

    @EntityGraph(attributePaths = {
            "languagesKnown",
            "interests"
    })
    Optional<UserProfile> findWithLanguagesAndInterestsByUserAccountId(
            UUID userAccountId
    );

    Page<UserProfile> findAll(Pageable pageable);

    @Query("""
SELECT up
FROM UserProfile up
WHERE up.userAccount.side <> :side
AND up.religion = :religion
AND up.id <> :profileId
AND up.profileStatus = com.shadiwaley.server.profile.domain.ProfileStatus.LIVE
""")
    Page<UserProfile> findAllEligibleMatches(
            @Param("side") Object side,
            @Param("religion") Object religion,
            @Param("profileId") UUID profileId,
            Pageable pageable
    );


    @Query("""
        SELECT profile
        FROM UserProfile profile
        JOIN profile.userAccount account
        WHERE account.side <> :sourceSide
        AND profile.id <> :sourceProfileId
        AND profile.profileStatus IN (
            com.shadiwaley.server.profile.domain.ProfileStatus.LIVE,
            com.shadiwaley.server.profile.domain.ProfileStatus.VERIFIED
        )
        """)
    Page<UserProfile> findAllEligibleMatches(
            @Param("sourceSide") com.shadiwaley.server.user.domain.UserSide sourceSide,
            @Param("sourceProfileId") UUID sourceProfileId,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p.education
        FROM UserProfile p
        WHERE p.education IS NOT NULL
        ORDER BY p.education
        """)
    List<String> findDistinctEducation();

    @Query("""
        SELECT DISTINCT p.professionType
        FROM UserProfile p
        WHERE p.professionType IS NOT NULL
        ORDER BY p.professionType
        """)
    List<String> findDistinctProfessionTypes();

    @Query("""
        SELECT DISTINCT p.familyType
        FROM UserProfile p
        WHERE p.familyType IS NOT NULL
        ORDER BY p.familyType
        """)
    List<String> findDistinctFamilyTypes();

}
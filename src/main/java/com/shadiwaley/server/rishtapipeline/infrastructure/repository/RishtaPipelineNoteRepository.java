package com.shadiwaley.server.rishtapipeline.infrastructure.repository;

import com.shadiwaley.server.rishtapipeline.infrastructure.entity.RishtaPipelineNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RishtaPipelineNoteRepository extends JpaRepository<RishtaPipelineNote, UUID> {

    Optional<RishtaPipelineNote> findTopByProposalIdOrderByCreatedAtDesc(UUID proposalId);

    List<RishtaPipelineNote> findByProposalIdOrderByCreatedAtDesc(UUID proposalId);
}
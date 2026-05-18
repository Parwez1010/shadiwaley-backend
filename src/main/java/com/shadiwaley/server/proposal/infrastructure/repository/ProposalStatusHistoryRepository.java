package com.shadiwaley.server.proposal.infrastructure.repository;

import com.shadiwaley.server.proposal.infrastructure.entity.ProposalStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProposalStatusHistoryRepository extends JpaRepository<ProposalStatusHistory, UUID> {

    List<ProposalStatusHistory> findByProposalIdOrderByCreatedAtAsc(UUID proposalId);
}
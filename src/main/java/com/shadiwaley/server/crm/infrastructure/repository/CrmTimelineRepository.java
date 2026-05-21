package com.shadiwaley.server.crm.infrastructure.repository;

import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrmTimelineRepository extends JpaRepository<CrmCaseTimeline, UUID> {

    List<CrmCaseTimeline> findByCrmCase_IdOrderByCreatedAtDesc(UUID crmCaseId);

}
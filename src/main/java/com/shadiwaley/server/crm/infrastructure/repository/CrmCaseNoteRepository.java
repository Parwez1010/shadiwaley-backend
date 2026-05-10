package com.shadiwaley.server.crm.infrastructure.repository;

import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrmCaseNoteRepository extends JpaRepository<CrmCaseNote, UUID> {

    List<CrmCaseNote> findByCrmCaseIdOrderByCreatedAtDesc(UUID crmCaseId);
}
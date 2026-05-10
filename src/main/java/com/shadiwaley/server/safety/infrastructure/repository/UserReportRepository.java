package com.shadiwaley.server.safety.infrastructure.repository;

import com.shadiwaley.server.safety.infrastructure.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, UUID> {
}
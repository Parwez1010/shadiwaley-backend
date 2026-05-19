package com.shadiwaley.server.revenue.infrastructure.repository;

import com.shadiwaley.server.revenue.infrastructure.entity.RevenuePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevenuePlanRepository extends JpaRepository<RevenuePlan, UUID> {

    Optional<RevenuePlan> findByCodeAndActiveTrue(String code);

    List<RevenuePlan> findByActiveTrueOrderBySortOrderAsc();
}
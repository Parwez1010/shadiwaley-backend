package com.shadiwaley.server.employee.infrastructure.repository;

import com.shadiwaley.server.employee.infrastructure.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, UUID> {

    Optional<EmployeeProfile> findByEmployeeAccountId(UUID employeeAccountId);

    boolean existsByEmployeeCode(String employeeCode);
}
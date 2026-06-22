package com.shadiwaley.server.employee.infrastructure.repository;

import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAccountRepository extends JpaRepository<EmployeeAccount, UUID> {

    Optional<EmployeeAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<EmployeeAccount> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<EmployeeAccount> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(EmployeeStatus status);

    List<EmployeeAccount> findByRoleAndDeletedAtIsNullOrderByCreatedAtDesc(EmployeeRole role);

    List<EmployeeAccount> findByAssignedDistrictIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(String assignedDistrict);

    long countByAccountStatus(
            com.shadiwaley.server.employee.domain.EmployeeAccountStatus accountStatus
    );
    long countByRole(com.shadiwaley.server.employee.domain.EmployeeRole role);

    List<EmployeeAccount>
    findByRoleInAndStatusAndDeletedAtIsNullOrderByFullNameAsc(
            List<EmployeeRole> roles,
            EmployeeStatus status
    );
}
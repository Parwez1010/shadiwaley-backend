package com.shadiwaley.server.safety.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.safety.dto.request.BlockUserRequest;
import com.shadiwaley.server.safety.dto.request.ReportUserRequest;
import com.shadiwaley.server.safety.infrastructure.entity.UserBlock;
import com.shadiwaley.server.safety.infrastructure.entity.UserReport;
import com.shadiwaley.server.safety.infrastructure.repository.UserBlockRepository;
import com.shadiwaley.server.safety.infrastructure.repository.UserReportRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSafetyService {

    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void blockUser(UUID blockedUserId, BlockUserRequest request) {
        UUID blockerUserId = AuthUser.getCurrentUserId();

        if (blockerUserId.equals(blockedUserId)) {
            throw new IllegalArgumentException("You cannot block yourself");
        }

        if (userBlockRepository.existsByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)) {
            return;
        }

        UserAccount blocker = getUser(blockerUserId);
        UserAccount blocked = getUser(blockedUserId);

        UserBlock block = new UserBlock();
        block.setBlockerUser(blocker);
        block.setBlockedUser(blocked);
        block.setReason(request.getReason());

        UserBlock saved = userBlockRepository.save(block);

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_ACCOUNT,
                blockedUserId,
                "User blocked",
                "blockId=" + saved.getId()
        );
    }

    @Transactional
    public void unblockUser(UUID blockedUserId) {
        UUID blockerUserId = AuthUser.getCurrentUserId();

        userBlockRepository.findByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)
                .ifPresent(userBlockRepository::delete);
    }

    @Transactional
    public void reportUser(UUID reportedUserId, ReportUserRequest request) {
        UUID reporterUserId = AuthUser.getCurrentUserId();

        if (reporterUserId.equals(reportedUserId)) {
            throw new IllegalArgumentException("You cannot report yourself");
        }

        UserAccount reporter = getUser(reporterUserId);
        UserAccount reported = getUser(reportedUserId);

        UserReport report = new UserReport();
        report.setReporterUser(reporter);
        report.setReportedUser(reported);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());

        UserReport saved = userReportRepository.save(report);

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_ACCOUNT,
                reportedUserId,
                "User reported: " + request.getReason(),
                "reportId=" + saved.getId()
        );
    }

    public boolean isBlockedBetween(UUID userA, UUID userB) {
        return userBlockRepository.existsByBlockerUserIdAndBlockedUserId(userA, userB)
                || userBlockRepository.existsByBlockerUserIdAndBlockedUserId(userB, userA);
    }

    private UserAccount getUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
package com.shadiwaley.server.customer.application.service;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatRoomRepository;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.customer.dto.response.*;
import com.shadiwaley.server.customer.infrastructure.repository.CustomerChatCountRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.notification.infrastructure.repository.UserNotificationRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.application.service.ProfileBrowseService;
import com.shadiwaley.server.profile.dto.response.ProfileCardResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.application.service.SubscriptionService;
import com.shadiwaley.server.subscription.dto.response.SubscriptionResponse;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAppService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final SubscriptionService subscriptionService;
    private final UserNotificationRepository notificationRepository;
    private final FamilyChatRoomRepository familyChatRoomRepository;
    private final RishtaRequestRepository rishtaRequestRepository;
    private final ProposalRepository proposalRepository;
    private final ProfileBrowseService profileBrowseService;
    private final CrmCaseRepository crmCaseRepository;


    private final CustomerChatCountRepository customerChatCountRepository;

    @Transactional(readOnly = true)
    public CustomerMeResponse getMe() {
        UUID userId = AuthUser.getCurrentActorId();

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        UserProfile profile = userProfileRepository
                .findByUserAccountId(user.getId())
                .orElse(null);

        ParentProfile parent = parentProfileRepository
                .findByUserAccountId(user.getId())
                .orElse(null);

        SubscriptionResponse subscription = null;

        try {
            subscription = subscriptionService.getMyPlan();
        } catch (Exception ignored) {
        }

        return CustomerMeResponse.builder()
                .userId(user.getId())
                .profileId(profile != null ? profile.getId() : null)
                .side(user.getSide())
                .phone(user.getPhone())

                .parentName(parent != null ? parent.getParentName() : null)
                .candidateName(profile != null ? profile.getCandidateFirstName() : null)

                .profileStatus(profile != null && profile.getProfileStatus() != null
                        ? profile.getProfileStatus().name()
                        : "INCOMPLETE")
                .completionPct(profile != null ? profile.getCompletionPct() : 0)

                .planCode(subscription != null && subscription.getPlanType() != null
                        ? subscription.getPlanType().name()
                        : null)

                .planName(subscription != null
                        ? subscription.getDisplayName()
                        : null)

                .subscriptionStatus(subscription != null && subscription.getStatus() != null
                        ? subscription.getStatus().name()
                        : null)

                .unreadNotificationsCount(notificationRepository.countByUserAccountIdAndReadFalse(user.getId()))
                .unreadChatCount(customerChatCountRepository.countUnreadForUser(user.getId()))

                .build();
    }


    @Transactional(readOnly = true)
    public CustomerDashboardResponse getDashboard() {

        CustomerMeResponse me = getMe();

        List<CustomerDashboardResponse.ChecklistItem> checklist = new ArrayList<>();

        checklist.add(
                CustomerDashboardResponse.ChecklistItem.builder()
                        .key("PROFILE")
                        .label("Complete Profile")
                        .completed(me.getCompletionPct() != null && me.getCompletionPct() >= 100)
                        .build()
        );

        checklist.add(
                CustomerDashboardResponse.ChecklistItem.builder()
                        .key("PLAN")
                        .label("Select Plan")
                        .completed(me.getPlanCode() != null)
                        .build()
        );

        long proposalsSent = me.getUserId() != null
                ? rishtaRequestRepository.findBySenderUserIdOrderByCreatedAtDesc(me.getUserId()).size()
                : 0;

        long proposalsReceived = me.getUserId() != null
                ? rishtaRequestRepository.findByReceiverUserIdOrderByCreatedAtDesc(me.getUserId()).size()
                : 0;

        long activeChats = me.getUserId() != null
                ? familyChatRoomRepository
                .findByBoyUserIdOrGirlUserIdOrderByUpdatedAtDesc(me.getUserId(), me.getUserId())
                .stream()
                .filter(room -> room.getStatus() == ChatRoomStatus.ACTIVE)
                .count()
                : 0;

        CustomerDashboardResponse.DashboardMetrics metrics =
                CustomerDashboardResponse.DashboardMetrics.builder()
                        .profileViews(0)
                        .matchesCount(0)
                        .proposalsSent(proposalsSent)
                        .proposalsReceived(proposalsReceived)
                        .activeChats(activeChats)
                        .unreadChats(me.getUnreadChatCount())
                        .build();

        List<CustomerDashboardResponse.DashboardActivityItem> recentActivity = new ArrayList<>();

        if (proposalsReceived > 0) {
            recentActivity.add(
                    CustomerDashboardResponse.DashboardActivityItem.builder()
                            .type("PROPOSAL_RECEIVED")
                            .title("New proposals received")
                            .description("You have received " + proposalsReceived + " proposal request(s).")
                            .createdAt(null)
                            .build()
            );
        }

        if (proposalsSent > 0) {
            recentActivity.add(
                    CustomerDashboardResponse.DashboardActivityItem.builder()
                            .type("PROPOSAL_SENT")
                            .title("Proposals sent")
                            .description("You have sent " + proposalsSent + " proposal request(s).")
                            .createdAt(null)
                            .build()
            );
        }

        if (activeChats > 0) {
            recentActivity.add(
                    CustomerDashboardResponse.DashboardActivityItem.builder()
                            .type("CHAT_ACTIVE")
                            .title("Active family chats")
                            .description("You have " + activeChats + " active chat(s).")
                            .createdAt(null)
                            .build()
            );
        }

        List<ProfileCardResponse> topMatches =
                profileBrowseService.getTopMatches(6);

        CustomerCrmAssignedInfoResponse crmAssignedInfo =
                getCrmAssignedInfo(me.getUserId());

        return CustomerDashboardResponse.builder()
                .me(me)
                .metrics(metrics)
                .profileChecklist(checklist)
                .recentActivity(recentActivity)
                .topMatches(topMatches)
                .crmAssignedInfo(crmAssignedInfo)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerNavigationResponse getNavigation() {
        CustomerMeResponse me = getMe();

        String status = me.getProfileStatus() != null
                ? me.getProfileStatus()
                : "INCOMPLETE";

        boolean approved = "APPROVED".equalsIgnoreCase(status)
                || "LIVE".equalsIgnoreCase(status);

        boolean incomplete = "INCOMPLETE".equalsIgnoreCase(status);

        boolean underReview = "UNDER_REVIEW".equalsIgnoreCase(status);

        boolean rejected = "REJECTED".equalsIgnoreCase(status);

        List<String> tabs;

        if (approved) {
            tabs = List.of(
                    "DASHBOARD",
                    "BROWSE",
                    "PROPOSALS",
                    "CHAT",
                    "PROFILE",
                    "SUBSCRIPTION"
            );
        } else {
            tabs = List.of(
                    "DASHBOARD",
                    "PROFILE",
                    "SUBSCRIPTION"
            );
        }

        return CustomerNavigationResponse.builder()
                .side(me.getSide() != null ? me.getSide().name() : null)
                .profileStatus(status)
                .canBrowse(approved)
                .canChat(approved)
                .canSendProposal(approved)
                .shouldCompleteProfile(incomplete)
                .shouldShowUnderReview(underReview)
                .shouldShowRejectedState(rejected)
                .tabs(tabs)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerBootstrapResponse getBootstrap() {

        CustomerMeResponse me = getMe();

        CustomerNavigationResponse navigation = getNavigation();

        CustomerDashboardResponse dashboard = getDashboard();

        return CustomerBootstrapResponse.builder()
                .me(me)
                .navigation(navigation)
                .dashboard(dashboard)
                .build();
    }

    private CustomerCrmAssignedInfoResponse getCrmAssignedInfo(UUID userId) {

        if (userId == null) {
            return null;
        }

        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(userId)
                .orElse(null);

        if (crmCase == null || crmCase.getAssignedEmployee() == null) {
            return null;
        }

        EmployeeAccount employee = crmCase.getAssignedEmployee();

        return CustomerCrmAssignedInfoResponse.builder()
                .crmCaseId(crmCase.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getFullName())
                .employeeEmail(employee.getEmail())
                .employeePhone(employee.getPhone())
                .employeeRole(employee.getRole() != null ? employee.getRole().name() : null)
                .caseStatus(crmCase.getStatus() != null ? crmCase.getStatus().name() : null)
                .caseStage(crmCase.getStage() != null ? crmCase.getStage().name() : null)
                .nextFollowUpAt(crmCase.getNextFollowUpAt())
                .build();
    }

}
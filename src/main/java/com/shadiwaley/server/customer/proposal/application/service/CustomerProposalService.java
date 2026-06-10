package com.shadiwaley.server.customer.proposal.application.service;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatRoomRepository;
import com.shadiwaley.server.customer.proposal.dto.request.SendCustomerProposalRequest;
import com.shadiwaley.server.customer.proposal.dto.response.*;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.rishta.application.service.RishtaRequestService;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.dto.request.CreateRishtaRequest;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerProposalService {

    private final RishtaRequestRepository rishtaRequestRepository;
    private final FamilyChatRoomRepository familyChatRoomRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final RishtaRequestService rishtaRequestService;


    public CustomerProposalSummaryResponse getSummary() {
        UUID userId = AuthUser.getCurrentActorId();

        List<RishtaRequest> sent =
                rishtaRequestRepository.findSentWithDetailsAndOptionalStatus(
                        userId,
                        null
                );

        List<RishtaRequest> received =
                rishtaRequestRepository.findReceivedWithDetailsAndOptionalStatus(
                        userId,
                        null
                );

        long sentPending = sent.stream()
                .filter(r -> r.getStatus() == RishtaRequestStatus.PENDING)
                .count();

        long receivedPending = received.stream()
                .filter(r -> r.getStatus() == RishtaRequestStatus.PENDING)
                .count();

        long accepted = java.util.stream.Stream.concat(sent.stream(), received.stream())
                .filter(r -> r.getStatus() == RishtaRequestStatus.ACCEPTED)
                .count();

        long rejected = java.util.stream.Stream.concat(sent.stream(), received.stream())
                .filter(r -> r.getStatus() == RishtaRequestStatus.REJECTED)
                .count();

        long expired = java.util.stream.Stream.concat(sent.stream(), received.stream())
                .filter(r -> r.getStatus() == RishtaRequestStatus.EXPIRED)
                .count();

        long activeChats = familyChatRoomRepository
                .findByBoyUserIdOrGirlUserIdOrderByUpdatedAtDesc(userId, userId)
                .stream()
                .filter(room -> room.getStatus() == ChatRoomStatus.ACTIVE)
                .count();

        return CustomerProposalSummaryResponse.builder()
                .sentTotal(sent.size())
                .receivedTotal(received.size())
                .sentPending(sentPending)
                .receivedPending(receivedPending)
                .accepted(accepted)
                .rejected(rejected)
                .expired(expired)
                .activeChats(activeChats)
                .build();
    }


    public CustomerProposalPageResponse getSent(
            int page,
            int size,
            RishtaRequestStatus status
    ) {
        UUID userId = AuthUser.getCurrentActorId();

        List<RishtaRequest> all =
                rishtaRequestRepository.findSentWithDetailsAndOptionalStatus(userId, status);

        return toPageResponse(all, page, size, true);
    }

    public CustomerProposalPageResponse getReceived(
            int page,
            int size,
            RishtaRequestStatus status
    ) {
        UUID userId = AuthUser.getCurrentActorId();

        List<RishtaRequest> all =
                rishtaRequestRepository.findReceivedWithDetailsAndOptionalStatus(userId, status);

        return toPageResponse(all, page, size, false);
    }


    public CustomerProposalDetailResponse getDetail(UUID requestId) {

        UUID userId = AuthUser.getCurrentActorId();

        RishtaRequest request = rishtaRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        boolean owner =
                request.getSenderUser().getId().equals(userId)
                        || request.getReceiverUser().getId().equals(userId);

        if (!owner) {
            throw new AccessDeniedException(
                    "You are not allowed to access this proposal"
            );
        }

        boolean sentView =
                request.getSenderUser().getId().equals(userId);

        return CustomerProposalDetailResponse.builder()
                .proposal(toItem(request, sentView))
                .fullProfileAvailableMessage(
                        request.getStatus() == RishtaRequestStatus.ACCEPTED
                                ? "Full profile available"
                                : "Accept proposal to unlock full profile access"
                )
                .build();
    }


    private CustomerProposalPageResponse toPageResponse(
            List<RishtaRequest> all,
            int page,
            int size,
            boolean sentView
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        int fromIndex = Math.min(safePage * safeSize, all.size());
        int toIndex = Math.min(fromIndex + safeSize, all.size());

        List<CustomerProposalItemResponse> items = all.subList(fromIndex, toIndex)
                .stream()
                .map(request -> toItem(request, sentView))
                .toList();

        int totalPages = all.isEmpty()
                ? 0
                : (int) Math.ceil((double) all.size() / safeSize);

        return CustomerProposalPageResponse.builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalElements(all.size())
                .totalPages(totalPages)
                .last(safePage + 1 >= totalPages)
                .build();
    }

    private CustomerProposalItemResponse toItem(
            RishtaRequest request,
            boolean sentView
    ) {
        UserAccount otherUser = sentView
                ? request.getReceiverUser()
                : request.getSenderUser();

        UserProfile otherProfile = sentView
                ? request.getReceiverProfile()
                : request.getSenderProfile();

        ParentProfile parent = otherUser != null
                ? parentProfileRepository.findByUserAccountId(otherUser.getId()).orElse(null)
                : null;

        UUID chatRoomId = familyChatRoomRepository
                .findByRishtaRequestId(request.getId())
                .map(room -> room.getId())
                .orElse(null);

        boolean pending = request.getStatus() == RishtaRequestStatus.PENDING;

        return CustomerProposalItemResponse.builder()
                .requestId(request.getId())

                .otherUserId(otherUser != null ? otherUser.getId() : null)
                .otherProfileId(otherProfile != null ? otherProfile.getId() : null)

                .candidateName(otherProfile != null ? otherProfile.getCandidateFirstName() : null)
                .parentName(parent != null ? parent.getParentName() : null)
                .phone(otherUser != null ? otherUser.getPhone() : null)
                .side(otherUser != null && otherUser.getSide() != null ? otherUser.getSide().name() : null)

                .age(otherProfile != null ? otherProfile.getCandidateAge() : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .education(otherProfile != null ? otherProfile.getEducation() : null)
                .professionTitle(otherProfile != null ? otherProfile.getProfessionTitle() : null)

                .status(request.getStatus())
                .senderNote(request.getSenderNote())

                .chatEnabled(request.isChatEnabled())
                .chatRoomId(chatRoomId)

                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())

                .canAccept(!sentView && pending)
                .canReject(!sentView && pending)
                .canCancel(sentView && pending)

                .build();
    }



    @Transactional
    public CustomerProposalDetailResponse accept(UUID requestId) {
        rishtaRequestService.accept(requestId);
        return getDetail(requestId);
    }

    @Transactional
    public CustomerProposalDetailResponse reject(UUID requestId) {
        rishtaRequestService.reject(requestId);
        return getDetail(requestId);
    }

    @Transactional
    public CustomerProposalDetailResponse cancel(UUID requestId) {
        rishtaRequestService.cancel(requestId);
        return getDetail(requestId);
    }

    @Transactional
    public void sendProposal(SendCustomerProposalRequest request) {
        CreateRishtaRequest rishtaRequest = new CreateRishtaRequest();
        rishtaRequest.setReceiverProfileId(request.getTargetProfileId());
        rishtaRequest.setSenderNote(request.getNote());

        rishtaRequestService.sendRequest(rishtaRequest);
    }

}
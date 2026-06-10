package com.shadiwaley.server.chat.dto.admin.response;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminChatRoomDetailResponse {

    private RoomInfo room;
    private ProfileInfo fromProfile;
    private ProfileInfo toProfile;
    private ProposalInfo proposal;
    private PipelineInfo pipeline;
    private ModerationInfo moderation;
    private StatsInfo stats;

    @Getter
    @Builder
    public static class RoomInfo {
        private UUID roomId;
        private UUID proposalId;
        private UUID pipelineId;
        private UUID crmCaseId;
        private ChatRoomStatus status;
        private String statusLabel;
        private UUID assignedEmployeeId;
        private String assignedEmployeeName;
        private Integer matchScore;
        private ProposalStatus proposalStatus;
        private RishtaPipelineStage pipelineStage;
        private String pipelineStageLabel;
        private String chatMode;

        private Boolean boyHasCrmSupport;

        private Boolean girlHasCrmSupport;

        private String expectedSpeaker;
        private Instant createdAt;
        private Instant lastMessageAt;
    }

    @Getter
    @Builder
    public static class ProfileInfo {
        private UUID profileId;
        private String candidateName;
        private String parentName;
        private String parentPhone;
        private String side;
        private Short age;
        private String district;
        private String state;
        private String caste;
        private String maslak;
        private String education;
        private String professionTitle;
        private String profilePhotoViewUrl;
    }

    @Getter
    @Builder
    public static class ProposalInfo {
        private UUID proposalId;
        private ProposalStatus status;
        private Integer matchScore;
        private Instant sentAt;
    }

    @Getter
    @Builder
    public static class PipelineInfo {
        private RishtaPipelineStage pipelineStage;
        private String pipelineStageLabel;
        private Instant nextFollowUpAt;
        private String lastNote;
    }

    @Getter
    @Builder
    public static class ModerationInfo {
        private boolean reported;
        private boolean blocked;
        private boolean needsAttention;
        private String lastReportReason;
    }

    @Getter
    @Builder
    public static class StatsInfo {
        private long messageCount;
        private long unreadCount;
        private long attachmentCount;
        private Instant lastMessageAt;
    }
}
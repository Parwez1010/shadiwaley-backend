package com.shadiwaley.server.rishtapipeline.application.service;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import org.springframework.stereotype.Component;

@Component
public class RishtaPipelineStageResolver {

    public RishtaPipelineStage resolve(ProposalStatus status) {
        if (status == null) {
            return RishtaPipelineStage.SENT;
        }

        return switch (status) {
            case SENT -> RishtaPipelineStage.SENT;
            case VIEWED -> RishtaPipelineStage.VIEWED;
            case INTERESTED -> RishtaPipelineStage.INTERESTED;
            case SHORTLISTED, MEETING_DISCUSSION -> RishtaPipelineStage.DISCUSSION;
            case ACCEPTED -> RishtaPipelineStage.ACCEPTED;
            case NOT_INTERESTED -> RishtaPipelineStage.NOT_INTERESTED;
            case REJECTED -> RishtaPipelineStage.REJECTED;
            case CANCELLED, EXPIRED -> RishtaPipelineStage.CLOSED;
        };
    }

    public String label(RishtaPipelineStage stage) {
        if (stage == null) {
            return "Sent";
        }

        return switch (stage) {
            case SENT -> "Sent";
            case VIEWED -> "Viewed";
            case INTERESTED -> "Interested";
            case DISCUSSION -> "In Discussion";
            case ACCEPTED -> "Accepted";
            case NOT_INTERESTED -> "Not Interested";
            case REJECTED -> "Rejected";
            case CLOSED -> "Closed";
        };
    }
}
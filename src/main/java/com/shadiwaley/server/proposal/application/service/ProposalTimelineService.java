package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseTimelineRepository;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProposalTimelineService {

    private final CrmCaseTimelineRepository crmCaseTimelineRepository;

    public void addProposalTimeline(
            CrmCase crmCase,
            CrmTimelineEventType eventType,
            String title,
            String description,
            Proposal proposal
    ) {
        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(crmCase);
        timeline.setEventType(eventType);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setActorEmployee(proposal.getLastUpdatedByEmployee());
        timeline.setActorName(proposal.getLastUpdatedByName());
        timeline.setOldValue(null);
        timeline.setNewValue(proposal.getStatus().name());
        timeline.setMetadata(
                "{\"proposalId\":\"" + proposal.getId()
                        + "\",\"fromProfileId\":\"" + proposal.getFromProfile().getId()
                        + "\",\"toProfileId\":\"" + proposal.getToProfile().getId()
                        + "\",\"matchScore\":" + proposal.getMatchScore()
                        + ",\"status\":\"" + proposal.getStatus().name()
                        + "\"}"
        );

        crmCaseTimelineRepository.save(timeline);
    }
}
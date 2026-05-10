package com.shadiwaley.server.rishta.application.service;

import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.rishta.infrastructure.repository.RishtaRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RishtaExpiryJob {

    private final RishtaRequestRepository rishtaRequestRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOldRequests() {
        var expired = rishtaRequestRepository.findByStatusAndExpiresAtBefore(
                RishtaRequestStatus.PENDING,
                Instant.now()
        );

        for (RishtaRequest request : expired) {
            request.setStatus(RishtaRequestStatus.EXPIRED);
            rishtaRequestRepository.save(request);

            notificationService.create(
                    request.getSenderUser().getId(),
                    NotificationType.SYSTEM_MESSAGE,
                    "Rishta request expired",
                    "One of your pending rishta requests has expired because there was no response.",
                    "/rishta/sent",
                    request.getId()
            );
        }
    }
}
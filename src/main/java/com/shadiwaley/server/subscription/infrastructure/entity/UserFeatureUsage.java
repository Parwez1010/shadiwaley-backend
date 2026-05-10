package com.shadiwaley.server.subscription.infrastructure.entity;

import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "user_feature_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_feature_usage_user_date",
                        columnNames = {"user_account_id", "usage_date"}
                )
        }
)
public class UserFeatureUsage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "rishta_requests_sent", nullable = false)
    private Integer rishtaRequestsSent;

    @Column(name = "usage_month", nullable = false, length = 7)
    private String usageMonth;

    @Column(name = "profile_views", nullable = false)
    private Integer profileViews;

    @Column(name = "active_chat_rooms", nullable = false)
    private Integer activeChatRooms;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (usageDate == null) {
            usageDate = LocalDate.now();
        }

        if (rishtaRequestsSent == null) {
            rishtaRequestsSent = 0;
        }

        if (profileViews == null) {
            profileViews = 0;
        }

        if (activeChatRooms == null) {
            activeChatRooms = 0;
        }
        if (usageMonth == null) {
            usageMonth = java.time.YearMonth.now().toString();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
package com.shadiwaley.server.communication.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunicationPermissionResponse {

    private boolean canViewAll;

    private boolean canAssign;

    private boolean canBulkAction;

    private boolean canReplyToChat;

    private boolean canReplyToSupport;

    private boolean canModerateChat;

    private boolean canCloseChat;

    private boolean canCloseSupport;

    private boolean canViewReports;

    private boolean canViewDashboard;

    private boolean canExport;
}
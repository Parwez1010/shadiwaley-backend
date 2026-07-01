package com.shadiwaley.server.communication.dto.request;

import com.shadiwaley.server.communication.domain.CommunicationBulkAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BulkCommunicationActionRequest {

    @NotNull
    private CommunicationBulkAction action;

    @NotEmpty
    private List<UUID> itemIds;

    /*
     * Used for ASSIGN
     */
    private UUID employeeId;

    /*
     * Used for CHANGE_PRIORITY
     */
    private String priority;

    /*
     * Used for CHANGE_STATUS
     */
    private String status;
}
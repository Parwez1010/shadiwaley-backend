package com.shadiwaley.server.autopilot.dto.request;

import com.shadiwaley.server.autopilot.domain.AutopilotSendMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendDispatchRequest {

    private AutopilotSendMode sendMode;

    private String note;

    private boolean createProposals = true;
}
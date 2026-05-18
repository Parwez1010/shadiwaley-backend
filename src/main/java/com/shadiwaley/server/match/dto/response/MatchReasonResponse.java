package com.shadiwaley.server.match.dto.response;

import com.shadiwaley.server.match.domain.MatchReasonType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchReasonResponse {

    private MatchReasonType type;

    private String message;

    private Integer scoreContribution;
}
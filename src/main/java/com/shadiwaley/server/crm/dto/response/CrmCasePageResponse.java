package com.shadiwaley.server.crm.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CrmCasePageResponse {
    private List<CrmCaseResponse> cases;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
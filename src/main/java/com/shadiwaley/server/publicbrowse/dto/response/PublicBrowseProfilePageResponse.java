package com.shadiwaley.server.publicbrowse.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicBrowseProfilePageResponse {

    private List<PublicBrowseProfileResponse> profiles;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
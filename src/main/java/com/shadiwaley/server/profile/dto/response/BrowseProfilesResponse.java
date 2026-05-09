package com.shadiwaley.server.profile.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BrowseProfilesResponse {
    private List<ProfileCardResponse> profiles;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
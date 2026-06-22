package com.shadiwaley.server.support.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SupportOptionsResponse {

    private List<String> categories;

    private List<String> priorities;

    private List<String> statuses;
}
package com.shadiwaley.server.chat.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminChatRoomPageResponse {

    private List<AdminChatRoomListItemResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
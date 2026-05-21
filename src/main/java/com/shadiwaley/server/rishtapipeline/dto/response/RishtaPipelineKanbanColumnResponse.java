package com.shadiwaley.server.rishtapipeline.dto.response;

import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RishtaPipelineKanbanColumnResponse {

    private RishtaPipelineStage stage;
    private String label;
    private long count;
    private List<RishtaPipelineItemResponse> items;
}
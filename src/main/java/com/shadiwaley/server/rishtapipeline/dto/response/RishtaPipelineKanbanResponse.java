package com.shadiwaley.server.rishtapipeline.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RishtaPipelineKanbanResponse {

    private List<RishtaPipelineKanbanColumnResponse> columns;
}
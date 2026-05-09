package com.shadiwaley.server.engagement.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.engagement.application.service.EngagementService;
import com.shadiwaley.server.engagement.application.service.MilestoneService;
import com.shadiwaley.server.engagement.dto.response.MilestoneResponse;
import com.shadiwaley.server.engagement.dto.response.TodayActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;
    private final MilestoneService milestoneService;

    @PostMapping("/session/start")
    public ApiResponse<Void> startSession() {

        engagementService.recordSessionStart();

        return ResponseFactory.success(
                "Session recorded successfully",
                null
        );
    }

    @GetMapping("/dashboard/today-action")
    public ApiResponse<TodayActionResponse> getTodayAction() {

        return ResponseFactory.success(
                "Today action fetched successfully",
                engagementService.getTodayAction()
        );
    }

    @GetMapping("/milestones")
    public ApiResponse<List<MilestoneResponse>> getMilestones() {

        return ResponseFactory.success(
                "Milestones fetched successfully",
                milestoneService.getMyMilestones()
        );
    }
}
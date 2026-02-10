package com.osc.devops.dashboard.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.dashboard.dto.DashboardDto;
import com.osc.devops.dashboard.service.DashboardService;
import com.osc.devops.packages.dto.PackageBuildDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardDto.Summary> getSummary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    @GetMapping("/recent-builds")
    public ApiResponse<List<PackageBuildDto.Response>> getRecentBuilds() {
        return ApiResponse.ok(dashboardService.getRecentBuilds());
    }
}

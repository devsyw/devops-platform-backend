package com.osc.devops.harbor.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.common.enums.SyncType;
import com.osc.devops.harbor.dto.HarborSyncDto;
import com.osc.devops.harbor.service.HarborService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/harbor")
@RequiredArgsConstructor
public class HarborController {

    private final HarborService harborService;

    @PostMapping("/sync")
    public ApiResponse<HarborSyncDto.SyncResult> manualSync() {
        return ApiResponse.ok(harborService.syncAllAddons(SyncType.MANUAL), "동기화가 완료되었습니다.");
    }

    @GetMapping("/sync-logs")
    public ApiResponse<Page<HarborSyncDto.LogResponse>> getSyncLogs(
            @RequestParam(required = false) Long addonId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(harborService.getSyncLogs(addonId, pageable));
    }
}

package com.osc.devops.addon.controller;

import com.osc.devops.addon.dto.AddonResponse;
import com.osc.devops.addon.service.AddonService;
import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.common.enums.AddonCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/addons")
@RequiredArgsConstructor
public class AddonController {

    private final AddonService addonService;

    @GetMapping
    public ApiResponse<List<AddonResponse>> getAddons(
            @RequestParam(required = false) AddonCategory category) {
        return ApiResponse.ok(addonService.getAddons(category));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddonResponse> getAddon(@PathVariable Long id) {
        return ApiResponse.ok(addonService.getAddon(id));
    }
}

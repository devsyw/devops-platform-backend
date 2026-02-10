package com.osc.devops.addon.controller;

import com.osc.devops.addon.dto.AddonDto;
import com.osc.devops.addon.dto.AddonResponse;
import com.osc.devops.addon.dto.AddonVersionResponse;
import com.osc.devops.addon.service.AddonService;
import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.common.enums.AddonCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addons")
@RequiredArgsConstructor
public class AddonController {

    private final AddonService addonService;

    // ======================== Addon CRUD ========================

    @GetMapping
    public ApiResponse<List<AddonResponse>> getAddons(
            @RequestParam(required = false) AddonCategory category,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        if (includeInactive) {
            return ApiResponse.ok(addonService.getAllAddons());
        }
        return ApiResponse.ok(addonService.getAddons(category));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddonResponse> getAddon(@PathVariable Long id) {
        return ApiResponse.ok(addonService.getAddon(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddonResponse> createAddon(@Valid @RequestBody AddonDto.CreateRequest request) {
        return ApiResponse.ok(addonService.createAddon(request), "애드온이 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<AddonResponse> updateAddon(
            @PathVariable Long id,
            @RequestBody AddonDto.UpdateRequest request) {
        return ApiResponse.ok(addonService.updateAddon(id, request), "애드온이 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddon(@PathVariable Long id) {
        addonService.deleteAddon(id);
        return ApiResponse.ok("애드온이 비활성화되었습니다.");
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<Void> activateAddon(@PathVariable Long id) {
        addonService.activateAddon(id);
        return ApiResponse.ok("애드온이 활성화되었습니다.");
    }

    // ======================== Version 관리 ========================

    @GetMapping("/{addonId}/versions")
    public ApiResponse<List<AddonVersionResponse>> getVersions(@PathVariable Long addonId) {
        return ApiResponse.ok(addonService.getVersions(addonId));
    }

    @PostMapping("/{addonId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddonVersionResponse> addVersion(
            @PathVariable Long addonId,
            @Valid @RequestBody AddonDto.VersionCreateRequest request) {
        return ApiResponse.ok(addonService.addVersion(addonId, request), "버전이 추가되었습니다.");
    }

    @DeleteMapping("/versions/{versionId}")
    public ApiResponse<Void> deleteVersion(@PathVariable Long versionId) {
        addonService.deleteVersion(versionId);
        return ApiResponse.ok("버전이 삭제되었습니다.");
    }
}

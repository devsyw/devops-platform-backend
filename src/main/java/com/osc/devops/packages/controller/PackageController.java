package com.osc.devops.packages.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.packages.dto.PackageBuildDto;
import com.osc.devops.packages.service.PackageBuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageBuildService buildService;

    @PostMapping("/build")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PackageBuildDto.Response> startBuild(
            @Valid @RequestBody PackageBuildDto.BuildRequest request) {
        return ApiResponse.ok(buildService.startBuild(request), "빌드가 시작되었습니다.");
    }

    @GetMapping
    public ApiResponse<Page<PackageBuildDto.Response>> getBuilds(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(buildService.getBuilds(customerId, projectId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PackageBuildDto.Response> getBuild(@PathVariable Long id) {
        return ApiResponse.ok(buildService.getBuild(id));
    }

    @GetMapping("/hash/{hash}")
    public ApiResponse<PackageBuildDto.Response> getBuildByHash(@PathVariable String hash) {
        return ApiResponse.ok(buildService.getBuildByHash(hash));
    }

    @GetMapping("/hash/{hash}/status")
    public ApiResponse<PackageBuildDto.Response> getBuildStatus(@PathVariable String hash) {
        return ApiResponse.ok(buildService.getBuildByHash(hash));
    }

    @GetMapping("/download/{hash}")
    public ResponseEntity<Resource> downloadPackage(@PathVariable String hash) {
        File file = buildService.getBuildFile(hash);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .body(resource);
    }
}

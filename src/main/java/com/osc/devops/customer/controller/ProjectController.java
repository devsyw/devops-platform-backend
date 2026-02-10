package com.osc.devops.customer.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.customer.dto.ProjectDto;
import com.osc.devops.customer.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<List<ProjectDto.Response>> getProjects(
            @PathVariable Long customerId,
            @RequestParam(required = false) Boolean includeInactive) {
        return ApiResponse.ok(Boolean.TRUE.equals(includeInactive)
                ? projectService.getAllProjects(customerId)
                : projectService.getProjects(customerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto.Response> getProject(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProject(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectDto.Response> createProject(
            @PathVariable Long customerId,
            @Valid @RequestBody ProjectDto.CreateRequest request) {
        return ApiResponse.ok(projectService.createProject(customerId, request), "프로젝트가 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDto.Response> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDto.UpdateRequest request) {
        return ApiResponse.ok(projectService.updateProject(id, request), "프로젝트가 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.ok("프로젝트가 비활성화되었습니다.");
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<ProjectDto.Response> activateProject(@PathVariable Long id) {
        return ApiResponse.ok(projectService.activateProject(id), "프로젝트가 활성화되었습니다.");
    }
}
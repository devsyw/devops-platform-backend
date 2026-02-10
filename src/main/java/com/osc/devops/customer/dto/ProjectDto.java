package com.osc.devops.customer.dto;

import com.osc.devops.customer.entity.Project;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ProjectDto {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "프로젝트명은 필수입니다")
        private String name;
        private String code;
        private String description;
        private String k8sVersion;
        private String namespace;
        private String domain;
        private String environment;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String code;
        private String description;
        private String k8sVersion;
        private String namespace;
        private String domain;
        private String environment;
    }

    @Getter @Builder @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long customerId;
        private String customerName;
        private String name;
        private String code;
        private String description;
        private String k8sVersion;
        private String namespace;
        private String domain;
        private String environment;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Project entity) {
            return Response.builder()
                    .id(entity.getId())
                    .customerId(entity.getCustomer().getId())
                    .customerName(entity.getCustomer().getName())
                    .name(entity.getName())
                    .code(entity.getCode())
                    .description(entity.getDescription())
                    .k8sVersion(entity.getK8sVersion())
                    .namespace(entity.getNamespace())
                    .domain(entity.getDomain())
                    .environment(entity.getEnvironment())
                    .isActive(entity.getIsActive())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}

package com.osc.devops.packages.dto;

import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.packages.entity.PackageBuild;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PackageBuildDto {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BuildRequest {
        private Long customerId;
        private Long projectId;

        @NotEmpty(message = "애드온을 1개 이상 선택해야 합니다")
        private List<AddonSelection> addons;

        private String namespace;
        private String domain;
        private boolean tlsEnabled;
        private boolean keycloakEnabled;
        private String builtBy;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AddonSelection {
        private Long addonId;
        private String addonName;
        private String version;        // 선택 버전 (null이면 latest)
        private String helmChartVersion;
    }

    @Getter @Builder @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long customerId;
        private String customerName;
        private Long projectId;
        private String projectName;
        private String buildHash;
        private String selectedAddons;
        private Long totalSize;
        private BuildStatus status;
        private String filePath;
        private String builtBy;
        private Boolean keycloakEnabled;
        private Boolean tlsEnabled;
        private String namespace;
        private String domain;
        private Integer progress;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;

        public static Response from(PackageBuild entity) {
            return Response.builder()
                    .id(entity.getId())
                    .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                    .customerName(entity.getCustomer() != null ? entity.getCustomer().getName() : null)
                    .projectId(entity.getProject() != null ? entity.getProject().getId() : null)
                    .projectName(entity.getProject() != null ? entity.getProject().getName() : null)
                    .buildHash(entity.getBuildHash())
                    .selectedAddons(entity.getSelectedAddons())
                    .totalSize(entity.getTotalSize())
                    .status(entity.getStatus())
                    .filePath(entity.getFilePath())
                    .builtBy(entity.getBuiltBy())
                    .keycloakEnabled(entity.getKeycloakEnabled())
                    .tlsEnabled(entity.getTlsEnabled())
                    .namespace(entity.getNamespace())
                    .domain(entity.getDomain())
                    .progress(entity.getProgress())
                    .expiresAt(entity.getExpiresAt())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}

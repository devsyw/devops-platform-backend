package com.osc.devops.addon.dto;

import com.osc.devops.addon.entity.Addon;
import com.osc.devops.addon.entity.AddonVersion;
import com.osc.devops.common.enums.AddonCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Builder @AllArgsConstructor
public class AddonResponse {
    private Long id;
    private String name;
    private String displayName;
    private AddonCategory category;
    private String description;
    private String iconUrl;
    private String upstreamImages;
    private String helmRepoUrl;
    private String helmChartName;
    private Boolean keycloakEnabled;
    private String keycloakClientTemplate;
    private String keycloakValuesTemplate;
    private Integer installOrder;
    private String dependencies;
    private Boolean isActive;
    private String latestVersion;
    private String latestHelmChartVersion;
    private List<AddonVersionResponse> versions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddonResponse from(Addon entity) {
        // 최신 버전 추출
        AddonVersion latest = entity.getVersions() != null
                ? entity.getVersions().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsLatest()))
                    .findFirst().orElse(null)
                : null;

        List<AddonVersionResponse> versionList = entity.getVersions() != null
                ? entity.getVersions().stream()
                    .map(AddonVersionResponse::from)
                    .collect(Collectors.toList())
                : List.of();

        return AddonResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .upstreamImages(entity.getUpstreamImages())
                .helmRepoUrl(entity.getHelmRepoUrl())
                .helmChartName(entity.getHelmChartName())
                .keycloakEnabled(entity.getKeycloakEnabled())
                .keycloakClientTemplate(entity.getKeycloakClientTemplate())
                .keycloakValuesTemplate(entity.getKeycloakValuesTemplate())
                .installOrder(entity.getInstallOrder())
                .dependencies(entity.getDependencies())
                .isActive(entity.getIsActive())
                .latestVersion(latest != null ? latest.getVersion() : null)
                .latestHelmChartVersion(latest != null ? latest.getHelmChartVersion() : null)
                .versions(versionList)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

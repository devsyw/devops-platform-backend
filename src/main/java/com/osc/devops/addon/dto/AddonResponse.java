package com.osc.devops.addon.dto;

import com.osc.devops.addon.entity.Addon;
import com.osc.devops.common.enums.AddonCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

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
    private Integer installOrder;
    private String dependencies;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static AddonResponse from(Addon entity) {
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
                .installOrder(entity.getInstallOrder())
                .dependencies(entity.getDependencies())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

package com.osc.devops.addon.dto;

import com.osc.devops.addon.entity.AddonVersion;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @AllArgsConstructor
public class AddonVersionResponse {
    private Long id;
    private Long addonId;
    private String version;
    private String imageTags;
    private String helmChartVersion;
    private Boolean isLatest;
    private String releaseNoteUrl;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;

    public static AddonVersionResponse from(AddonVersion entity) {
        return AddonVersionResponse.builder()
                .id(entity.getId())
                .addonId(entity.getAddon().getId())
                .version(entity.getVersion())
                .imageTags(entity.getImageTags())
                .helmChartVersion(entity.getHelmChartVersion())
                .isLatest(entity.getIsLatest())
                .releaseNoteUrl(entity.getReleaseNoteUrl())
                .syncedAt(entity.getSyncedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

package com.osc.devops.addon.dto;

import com.osc.devops.common.enums.AddonCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class AddonDto {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "시스템 이름은 필수입니다")
        private String name;
        @NotBlank(message = "표시 이름은 필수입니다")
        private String displayName;
        @NotNull(message = "카테고리는 필수입니다")
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
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
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
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VersionCreateRequest {
        @NotBlank(message = "버전은 필수입니다")
        private String version;
        private String imageTags;
        private String helmChartVersion;
        private Boolean isLatest;
        private String releaseNoteUrl;
    }
}

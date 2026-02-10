package com.osc.devops.harbor.dto;

import com.osc.devops.common.enums.SyncStatus;
import com.osc.devops.common.enums.SyncType;
import com.osc.devops.harbor.entity.HarborSyncLog;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class HarborSyncDto {

    @Getter @Builder @AllArgsConstructor
    public static class SyncResult {
        private int totalAddons;
        private int successCount;
        private int failCount;
        private int newVersionsFound;
        private List<String> errors;
        private LocalDateTime syncedAt;
    }

    @Getter @Builder @AllArgsConstructor
    public static class LogResponse {
        private Long id;
        private Long addonId;
        private String addonName;
        private SyncType syncType;
        private SyncStatus status;
        private String newVersionsFound;
        private String errorMessage;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public static LogResponse from(HarborSyncLog entity) {
            return LogResponse.builder()
                    .id(entity.getId())
                    .addonId(entity.getAddon().getId())
                    .addonName(entity.getAddon().getDisplayName())
                    .syncType(entity.getSyncType())
                    .status(entity.getStatus())
                    .newVersionsFound(entity.getNewVersionsFound())
                    .errorMessage(entity.getErrorMessage())
                    .startedAt(entity.getStartedAt())
                    .completedAt(entity.getCompletedAt())
                    .build();
        }
    }
}

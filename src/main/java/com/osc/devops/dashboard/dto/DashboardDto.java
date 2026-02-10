package com.osc.devops.dashboard.dto;

import lombok.*;

public class DashboardDto {

    @Getter @Builder @AllArgsConstructor
    public static class Summary {
        private long customerCount;
        private long addonCount;
        private long buildCount;
        private long successBuildCount;
        private long activeCertCount;
        private long expiringCertCount;
        private long newVersionCount;
    }
}

package com.osc.devops.notification.dto;

import com.osc.devops.common.enums.NotificationType;
import com.osc.devops.notification.entity.Notification;
import lombok.*;
import java.time.LocalDateTime;

public class NotificationDto {

    @Getter @Builder @AllArgsConstructor
    public static class Response {
        private Long id;
        private NotificationType type;
        private String title;
        private String message;
        private Long addonId;
        private String addonName;
        private Long customerId;
        private String customerName;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static Response from(Notification entity) {
            return Response.builder()
                    .id(entity.getId())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .message(entity.getMessage())
                    .addonId(entity.getAddon() != null ? entity.getAddon().getId() : null)
                    .addonName(entity.getAddon() != null ? entity.getAddon().getDisplayName() : null)
                    .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                    .customerName(entity.getCustomer() != null ? entity.getCustomer().getName() : null)
                    .isRead(entity.getIsRead())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}

package com.osc.devops.notification.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.notification.dto.NotificationDto;
import com.osc.devops.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<NotificationDto.Response>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(notificationService.getNotifications(pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(notificationService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok("읽음 처리되었습니다.");
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.ok("전체 읽음 처리되었습니다.");
    }
}

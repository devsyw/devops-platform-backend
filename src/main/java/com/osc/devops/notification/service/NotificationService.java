package com.osc.devops.notification.service;

import com.osc.devops.common.exception.NotFoundException;
import com.osc.devops.notification.dto.NotificationDto;
import com.osc.devops.notification.entity.Notification;
import com.osc.devops.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationDto.Response> getNotifications(Pageable pageable) {
        return notificationRepository.findByOrderByCreatedAtDesc(pageable)
                .map(NotificationDto.Response::from);
    }

    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        Notification noti = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));
        noti.setIsRead(true);
        notificationRepository.save(noti);
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.findByOrderByCreatedAtDesc(Pageable.unpaged())
                .filter(n -> !n.getIsRead())
                .forEach(n -> { n.setIsRead(true); notificationRepository.save(n); });
    }
}

package com.osc.devops.notification.repository;

import com.osc.devops.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByOrderByCreatedAtDesc(Pageable pageable);
    long countByIsReadFalse();
}

package com.osc.devops.harbor.repository;

import com.osc.devops.harbor.entity.HarborSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HarborSyncLogRepository extends JpaRepository<HarborSyncLog, Long> {
    Page<HarborSyncLog> findByOrderByStartedAtDesc(Pageable pageable);
    Page<HarborSyncLog> findByAddonIdOrderByStartedAtDesc(Long addonId, Pageable pageable);
}

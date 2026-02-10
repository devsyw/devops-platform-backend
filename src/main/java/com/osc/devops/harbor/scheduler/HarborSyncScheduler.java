package com.osc.devops.harbor.scheduler;

import com.osc.devops.common.enums.SyncType;
import com.osc.devops.harbor.dto.HarborSyncDto;
import com.osc.devops.harbor.service.HarborService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HarborSyncScheduler {

    private final HarborService harborService;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void dailySync() {
        log.info("Harbor 일일 동기화 시작");
        try {
            HarborSyncDto.SyncResult result = harborService.syncAllAddons(SyncType.DAILY);
            log.info("Harbor 일일 동기화 완료: success={}, fail={}, newVersions={}",
                    result.getSuccessCount(), result.getFailCount(), result.getNewVersionsFound());
        } catch (Exception e) {
            log.error("Harbor 일일 동기화 실패", e);
        }
    }
}

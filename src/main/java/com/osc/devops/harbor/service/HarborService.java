package com.osc.devops.harbor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.devops.addon.entity.Addon;
import com.osc.devops.addon.entity.AddonVersion;
import com.osc.devops.addon.repository.AddonRepository;
import com.osc.devops.addon.repository.AddonVersionRepository;
import com.osc.devops.common.enums.SyncStatus;
import com.osc.devops.common.enums.SyncType;
import com.osc.devops.harbor.dto.HarborSyncDto;
import com.osc.devops.harbor.entity.HarborSyncLog;
import com.osc.devops.harbor.repository.HarborSyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HarborService {

    private final AddonRepository addonRepository;
    private final AddonVersionRepository addonVersionRepository;
    private final HarborSyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${harbor.url:http://localhost:8082}")
    private String harborUrl;

    @Value("${harbor.username:admin}")
    private String harborUsername;

    @Value("${harbor.password:Harbor12345}")
    private String harborPassword;

    @Value("${harbor.project:devops-upstream}")
    private String harborProject;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 전체 애드온의 upstream 이미지를 Harbor에서 확인하고 새 버전 감지
     */
    @Transactional
    public HarborSyncDto.SyncResult syncAllAddons(SyncType syncType) {
        List<Addon> addons = addonRepository.findByIsActiveTrueOrderByInstallOrderAsc();
        int totalNew = 0;
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (Addon addon : addons) {
            try {
                int newVersions = syncAddon(addon, syncType);
                totalNew += newVersions;
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add(addon.getName() + ": " + e.getMessage());
                log.error("Harbor 동기화 실패: addon={}", addon.getName(), e);
            }
        }

        return HarborSyncDto.SyncResult.builder()
                .totalAddons(addons.size())
                .successCount(successCount)
                .failCount(failCount)
                .newVersionsFound(totalNew)
                .errors(errors)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 개별 애드온 동기화
     */
    @Transactional
    public int syncAddon(Addon addon, SyncType syncType) {
        HarborSyncLog syncLog = HarborSyncLog.builder()
                .addon(addon)
                .syncType(syncType)
                .status(SyncStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();
        syncLog = syncLogRepository.save(syncLog);

        try {
            List<String> upstreamImages = parseUpstreamImages(addon.getUpstreamImages());
            if (upstreamImages.isEmpty()) {
                syncLog.setStatus(SyncStatus.SUCCESS);
                syncLog.setCompletedAt(LocalDateTime.now());
                syncLogRepository.save(syncLog);
                return 0;
            }

            // Harbor API로 각 이미지의 태그 목록 조회
            Set<String> discoveredTags = new HashSet<>();
            for (String image : upstreamImages) {
                List<String> tags = fetchHarborTags(image);
                discoveredTags.addAll(tags);
            }

            // 기존 버전과 비교하여 새 버전 감지
            List<String> existingVersions = addonVersionRepository
                    .findByAddonIdOrderByCreatedAtDesc(addon.getId())
                    .stream()
                    .map(AddonVersion::getVersion)
                    .toList();

            List<String> newVersions = discoveredTags.stream()
                    .filter(tag -> !existingVersions.contains(tag))
                    .filter(tag -> isValidVersion(tag))
                    .sorted(Comparator.reverseOrder())
                    .limit(5) // 최대 5개 신규 버전
                    .toList();

            // 신규 버전 등록
            for (String ver : newVersions) {
                // 기존 latest 해제
                addonVersionRepository.findByAddonIdAndIsLatestTrue(addon.getId())
                        .ifPresent(v -> { v.setIsLatest(false); addonVersionRepository.save(v); });

                AddonVersion newVer = AddonVersion.builder()
                        .addon(addon)
                        .version(ver)
                        .isLatest(true)
                        .syncedAt(LocalDateTime.now())
                        .build();
                addonVersionRepository.save(newVer);
            }

            syncLog.setStatus(SyncStatus.SUCCESS);
            syncLog.setNewVersionsFound(objectMapper.writeValueAsString(newVersions));
            syncLog.setCompletedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);

            if (!newVersions.isEmpty()) {
                log.info("Harbor 동기화: addon={}, 신규버전={}", addon.getName(), newVersions);
            }
            return newVersions.size();

        } catch (Exception e) {
            syncLog.setStatus(SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setCompletedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);
            throw new RuntimeException("동기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Harbor API: 특정 이미지의 태그 목록 조회
     */
    private List<String> fetchHarborTags(String imageName) {
        try {
            // imageName 예: quay.io/keycloak/keycloak → repo name에서 마지막 부분 사용
            String repoName = imageName.contains("/") ?
                    imageName.substring(imageName.lastIndexOf("/") + 1) : imageName;

            String url = harborUrl + "/api/v2.0/projects/" + harborProject
                    + "/repositories/" + repoName + "/artifacts?page_size=20";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(harborUsername, harborPassword);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode artifacts = objectMapper.readTree(response.getBody());
                List<String> tags = new ArrayList<>();
                for (JsonNode artifact : artifacts) {
                    JsonNode tagsNode = artifact.get("tags");
                    if (tagsNode != null && tagsNode.isArray()) {
                        for (JsonNode tag : tagsNode) {
                            String tagName = tag.get("name").asText();
                            tags.add(tagName);
                        }
                    }
                }
                return tags;
            }
        } catch (Exception e) {
            log.warn("Harbor 태그 조회 실패: image={}, error={}", imageName, e.getMessage());
        }
        return Collections.emptyList();
    }

    // ======================== 조회 ========================

    @Transactional(readOnly = true)
    public Page<HarborSyncDto.LogResponse> getSyncLogs(Long addonId, Pageable pageable) {
        Page<HarborSyncLog> page = (addonId != null)
                ? syncLogRepository.findByAddonIdOrderByStartedAtDesc(addonId, pageable)
                : syncLogRepository.findByOrderByStartedAtDesc(pageable);
        return page.map(HarborSyncDto.LogResponse::from);
    }

    // ======================== 유틸리티 ========================

    private List<String> parseUpstreamImages(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean isValidVersion(String tag) {
        if (tag == null) return false;
        // "latest", "main", "master" 등 비버전 태그 제외
        if (tag.matches("(?i)(latest|main|master|dev|nightly|edge|alpha|beta|rc)")) return false;
        // 숫자로 시작하는 태그만 허용 (v1.0, 1.0.0, 26.0.7 등)
        return tag.matches("v?\\d+.*");
    }
}

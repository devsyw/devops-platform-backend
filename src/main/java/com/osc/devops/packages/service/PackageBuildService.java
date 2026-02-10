package com.osc.devops.packages.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.devops.addon.entity.Addon;
import com.osc.devops.addon.entity.AddonVersion;
import com.osc.devops.addon.repository.AddonRepository;
import com.osc.devops.addon.repository.AddonVersionRepository;
import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.common.exception.BadRequestException;
import com.osc.devops.common.exception.NotFoundException;
import com.osc.devops.customer.entity.Customer;
import com.osc.devops.customer.entity.Project;
import com.osc.devops.customer.repository.CustomerRepository;
import com.osc.devops.customer.repository.ProjectRepository;
import com.osc.devops.packages.dto.PackageBuildDto;
import com.osc.devops.packages.entity.PackageBuild;
import com.osc.devops.packages.repository.PackageBuildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageBuildService {

    private final PackageBuildRepository buildRepository;
    private final AddonRepository addonRepository;
    private final AddonVersionRepository addonVersionRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final PackageBuildExecutor buildExecutor;
    private final ObjectMapper objectMapper;

    @Value("${package.build.expire-days:30}")
    private int expireDays;

    // ======================== 빌드 시작 ========================

    @Transactional
    public PackageBuildDto.Response startBuild(PackageBuildDto.BuildRequest request) {
        Customer customer = null;
        Project project = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다."));
        }
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));
        }

        String buildHash = generateBuildHash(request);
        List<Map<String, Object>> addonInfoList = resolveAddonSelections(request.getAddons());

        String selectedAddonsJson;
        try {
            selectedAddonsJson = objectMapper.writeValueAsString(addonInfoList);
        } catch (Exception e) {
            throw new BadRequestException("애드온 정보 직렬화 실패");
        }

        PackageBuild build = PackageBuild.builder()
                .customer(customer)
                .project(project)
                .buildHash(buildHash)
                .selectedAddons(selectedAddonsJson)
                .builtBy(request.getBuiltBy())
                .keycloakEnabled(request.isKeycloakEnabled())
                .tlsEnabled(request.isTlsEnabled())
                .namespace(request.getNamespace())
                .domain(request.getDomain())
                .deployEnv(request.getDeployEnv())
                .registryUrl(request.getRegistryUrl())
                .platform(request.getPlatform())
                .status(BuildStatus.BUILDING)
                .progress(0)
                .expiresAt(LocalDateTime.now().plusDays(expireDays))
                .build();
        build = buildRepository.save(build);

        // 트랜잭션 커밋 후 비동기 빌드 실행 (커밋 전 @Async 호출 시 레코드 조회 실패 방지)
        final Long buildId = build.getId();
        final List<Map<String, Object>> finalAddonInfoList = addonInfoList;
        final PackageBuildDto.BuildRequest finalRequest = request;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                buildExecutor.executeBuild(buildId, finalAddonInfoList, finalRequest);
            }
        });

        return PackageBuildDto.Response.from(build);
    }

    // ======================== 조회 ========================

    @Transactional(readOnly = true)
    public Page<PackageBuildDto.Response> getBuilds(Long customerId, Long projectId, Pageable pageable) {
        Page<PackageBuild> page;
        if (projectId != null) {
            page = buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        } else if (customerId != null) {
            page = buildRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        } else {
            page = buildRepository.findByOrderByCreatedAtDesc(pageable);
        }
        return page.map(PackageBuildDto.Response::from);
    }

    @Transactional(readOnly = true)
    public PackageBuildDto.Response getBuild(Long id) {
        return buildRepository.findById(id)
                .map(PackageBuildDto.Response::from)
                .orElseThrow(() -> new NotFoundException("빌드를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public PackageBuildDto.Response getBuildByHash(String hash) {
        return buildRepository.findByBuildHash(hash)
                .map(PackageBuildDto.Response::from)
                .orElseThrow(() -> new NotFoundException("빌드를 찾을 수 없습니다."));
    }

    public File getBuildFile(String hash) {
        PackageBuild build = buildRepository.findByBuildHash(hash)
                .orElseThrow(() -> new NotFoundException("빌드를 찾을 수 없습니다."));
        if (build.getStatus() != BuildStatus.SUCCESS || build.getFilePath() == null) {
            throw new BadRequestException("빌드가 완료되지 않았거나 파일이 존재하지 않습니다.");
        }
        File file = new File(build.getFilePath());
        if (!file.exists()) {
            throw new NotFoundException("빌드 파일이 존재하지 않습니다.");
        }
        return file;
    }

    // ======================== 유틸리티 ========================

    private List<Map<String, Object>> resolveAddonSelections(List<PackageBuildDto.AddonSelection> selections) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PackageBuildDto.AddonSelection sel : selections) {
            Addon addon = addonRepository.findById(sel.getAddonId())
                    .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + sel.getAddonId()));

            String version = sel.getVersion();
            String helmChartVersion = sel.getHelmChartVersion();
            String imageTags = null;

            // 버전 미지정 시 latest 버전 조회
            AddonVersion addonVersion = null;
            if (version == null || version.isEmpty()) {
                addonVersion = addonVersionRepository.findByAddonIdAndIsLatestTrue(addon.getId()).orElse(null);
            } else {
                addonVersion = addonVersionRepository.findByAddonIdAndVersion(addon.getId(), version).orElse(null);
            }
            if (addonVersion != null) {
                version = addonVersion.getVersion();
                helmChartVersion = addonVersion.getHelmChartVersion();
                imageTags = addonVersion.getImageTags();
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("addonId", addon.getId());
            info.put("name", addon.getName());
            info.put("displayName", addon.getDisplayName());
            info.put("category", addon.getCategory().name());
            info.put("version", version);
            info.put("helmRepoUrl", addon.getHelmRepoUrl());
            info.put("helmChartName", addon.getHelmChartName());
            info.put("helmChartVersion", helmChartVersion);
            info.put("upstreamImages", addon.getUpstreamImages());
            info.put("imageTags", imageTags);
            info.put("keycloakEnabled", addon.getKeycloakEnabled());
            info.put("installOrder", addon.getInstallOrder());
            result.add(info);
        }
        return result;
    }

    private String generateBuildHash(PackageBuildDto.BuildRequest request) {
        try {
            String input = request.getCustomerId() + "-" + request.getProjectId() + "-"
                    + System.currentTimeMillis() + "-" + UUID.randomUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02x", hash[i]));
            return hex.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
}
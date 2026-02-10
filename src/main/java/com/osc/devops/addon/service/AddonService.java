package com.osc.devops.addon.service;

import com.osc.devops.addon.dto.AddonDto;
import com.osc.devops.addon.dto.AddonResponse;
import com.osc.devops.addon.dto.AddonVersionResponse;
import com.osc.devops.addon.entity.Addon;
import com.osc.devops.addon.entity.AddonVersion;
import com.osc.devops.addon.repository.AddonRepository;
import com.osc.devops.addon.repository.AddonVersionRepository;
import com.osc.devops.common.enums.AddonCategory;
import com.osc.devops.common.exception.BadRequestException;
import com.osc.devops.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddonService {

    private final AddonRepository addonRepository;
    private final AddonVersionRepository addonVersionRepository;

    // ======================== Addon CRUD ========================

    public List<AddonResponse> getAddons(AddonCategory category) {
        List<Addon> addons = (category != null)
                ? addonRepository.findByCategoryAndIsActiveTrueOrderByInstallOrderAsc(category)
                : addonRepository.findByIsActiveTrueOrderByInstallOrderAsc();
        return addons.stream().map(AddonResponse::from).collect(Collectors.toList());
    }

    public List<AddonResponse> getAllAddons() {
        return addonRepository.findAll().stream()
                .map(AddonResponse::from).collect(Collectors.toList());
    }

    public AddonResponse getAddon(Long id) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + id));
        return AddonResponse.from(addon);
    }

    @Transactional
    public AddonResponse createAddon(AddonDto.CreateRequest request) {
        addonRepository.findByName(request.getName()).ifPresent(a -> {
            throw new BadRequestException("이미 존재하는 애드온 이름입니다: " + request.getName());
        });

        Addon addon = Addon.builder()
                .name(request.getName())
                .displayName(request.getDisplayName())
                .category(request.getCategory())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .upstreamImages(request.getUpstreamImages())
                .helmRepoUrl(request.getHelmRepoUrl())
                .helmChartName(request.getHelmChartName())
                .keycloakEnabled(request.getKeycloakEnabled() != null ? request.getKeycloakEnabled() : false)
                .keycloakClientTemplate(request.getKeycloakClientTemplate())
                .keycloakValuesTemplate(request.getKeycloakValuesTemplate())
                .installOrder(request.getInstallOrder() != null ? request.getInstallOrder() : 50)
                .dependencies(request.getDependencies())
                .build();
        return AddonResponse.from(addonRepository.save(addon));
    }

    @Transactional
    public AddonResponse updateAddon(Long id, AddonDto.UpdateRequest request) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + id));

        if (request.getDisplayName() != null) addon.setDisplayName(request.getDisplayName());
        if (request.getCategory() != null) addon.setCategory(request.getCategory());
        if (request.getDescription() != null) addon.setDescription(request.getDescription());
        if (request.getIconUrl() != null) addon.setIconUrl(request.getIconUrl());
        if (request.getUpstreamImages() != null) addon.setUpstreamImages(request.getUpstreamImages());
        if (request.getHelmRepoUrl() != null) addon.setHelmRepoUrl(request.getHelmRepoUrl());
        if (request.getHelmChartName() != null) addon.setHelmChartName(request.getHelmChartName());
        if (request.getKeycloakEnabled() != null) addon.setKeycloakEnabled(request.getKeycloakEnabled());
        if (request.getKeycloakClientTemplate() != null) addon.setKeycloakClientTemplate(request.getKeycloakClientTemplate());
        if (request.getKeycloakValuesTemplate() != null) addon.setKeycloakValuesTemplate(request.getKeycloakValuesTemplate());
        if (request.getInstallOrder() != null) addon.setInstallOrder(request.getInstallOrder());
        if (request.getDependencies() != null) addon.setDependencies(request.getDependencies());
        return AddonResponse.from(addonRepository.save(addon));
    }

    @Transactional
    public void deleteAddon(Long id) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + id));
        addon.setIsActive(false);
        addonRepository.save(addon);
    }

    @Transactional
    public void activateAddon(Long id) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + id));
        addon.setIsActive(true);
        addonRepository.save(addon);
    }

    // ======================== Version 관리 ========================

    public List<AddonVersionResponse> getVersions(Long addonId) {
        return addonVersionRepository.findByAddonIdOrderByCreatedAtDesc(addonId)
                .stream().map(AddonVersionResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public AddonVersionResponse addVersion(Long addonId, AddonDto.VersionCreateRequest request) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + addonId));

        if (Boolean.TRUE.equals(request.getIsLatest())) {
            addonVersionRepository.findByAddonIdAndIsLatestTrue(addonId)
                    .ifPresent(v -> {
                        v.setIsLatest(false);
                        addonVersionRepository.save(v);
                    });
        }

        AddonVersion version = AddonVersion.builder()
                .addon(addon)
                .version(request.getVersion())
                .imageTags(request.getImageTags())
                .helmChartVersion(request.getHelmChartVersion())
                .isLatest(request.getIsLatest() != null ? request.getIsLatest() : false)
                .releaseNoteUrl(request.getReleaseNoteUrl())
                .syncedAt(LocalDateTime.now())
                .build();
        return AddonVersionResponse.from(addonVersionRepository.save(version));
    }

    @Transactional
    public void deleteVersion(Long versionId) {
        addonVersionRepository.deleteById(versionId);
    }

    // ======================== 통계 ========================

    public long countActiveAddons() {
        return addonRepository.findByIsActiveTrueOrderByInstallOrderAsc().size();
    }

    public long countNewVersions() {
        return addonVersionRepository.findByIsLatestTrue().stream()
                .filter(v -> v.getSyncedAt() != null &&
                        v.getSyncedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
    }
}

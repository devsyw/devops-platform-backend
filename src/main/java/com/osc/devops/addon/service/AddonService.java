package com.osc.devops.addon.service;

import com.osc.devops.addon.dto.AddonResponse;
import com.osc.devops.addon.entity.Addon;
import com.osc.devops.addon.repository.AddonRepository;
import com.osc.devops.addon.repository.AddonVersionRepository;
import com.osc.devops.common.enums.AddonCategory;
import com.osc.devops.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddonService {

    private final AddonRepository addonRepository;
    private final AddonVersionRepository addonVersionRepository;

    public List<AddonResponse> getAddons(AddonCategory category) {
        List<Addon> addons = (category != null)
                ? addonRepository.findByCategoryAndIsActiveTrueOrderByInstallOrderAsc(category)
                : addonRepository.findByIsActiveTrueOrderByInstallOrderAsc();
        return addons.stream().map(AddonResponse::from).collect(Collectors.toList());
    }

    public AddonResponse getAddon(Long id) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("애드온을 찾을 수 없습니다. id=" + id));
        return AddonResponse.from(addon);
    }
}

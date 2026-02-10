package com.osc.devops.addon.repository;

import com.osc.devops.addon.entity.AddonVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddonVersionRepository extends JpaRepository<AddonVersion, Long> {
    List<AddonVersion> findByAddonIdOrderByCreatedAtDesc(Long addonId);
    Optional<AddonVersion> findByAddonIdAndIsLatestTrue(Long addonId);
    Optional<AddonVersion> findByAddonIdAndVersion(Long addonId, String version);
    List<AddonVersion> findByIsLatestTrue();
}
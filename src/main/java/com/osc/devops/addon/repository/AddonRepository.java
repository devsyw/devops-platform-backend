package com.osc.devops.addon.repository;

import com.osc.devops.addon.entity.Addon;
import com.osc.devops.common.enums.AddonCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddonRepository extends JpaRepository<Addon, Long> {
    List<Addon> findByIsActiveTrueOrderByInstallOrderAsc();
    List<Addon> findByCategoryAndIsActiveTrueOrderByInstallOrderAsc(AddonCategory category);
    Optional<Addon> findByName(String name);
}

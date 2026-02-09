package com.osc.devops.installation.repository;

import com.osc.devops.installation.entity.InstallAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstallAddonRepository extends JpaRepository<InstallAddon, Long> {
    List<InstallAddon> findByInstallationId(Long installationId);
}

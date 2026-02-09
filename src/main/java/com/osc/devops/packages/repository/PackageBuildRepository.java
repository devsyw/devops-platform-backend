package com.osc.devops.packages.repository;

import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.packages.entity.PackageBuild;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PackageBuildRepository extends JpaRepository<PackageBuild, Long> {
    Optional<PackageBuild> findByBuildHash(String buildHash);
    Page<PackageBuild> findByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(BuildStatus status);
}

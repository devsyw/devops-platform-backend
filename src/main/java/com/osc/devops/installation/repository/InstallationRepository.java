package com.osc.devops.installation.repository;

import com.osc.devops.installation.entity.Installation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface InstallationRepository extends JpaRepository<Installation, Long> {
    Page<Installation> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    List<Installation> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);
}

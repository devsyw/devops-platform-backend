package com.osc.devops.customer.repository;

import com.osc.devops.customer.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCustomerIdAndIsActiveTrueOrderByCreatedAtDesc(Long customerId);
    long countByCustomerIdAndIsActiveTrue(Long customerId);
}

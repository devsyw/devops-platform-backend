package com.osc.devops.certificate.repository;

import com.osc.devops.certificate.entity.Certificate;
import com.osc.devops.common.enums.CertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Page<Certificate> findByCustomerId(Long customerId, Pageable pageable);
    List<Certificate> findByStatusAndExpiresAtBefore(CertStatus status, LocalDate date);
    List<Certificate> findByStatusAndExpiresAtBetween(CertStatus status, LocalDate from, LocalDate to);
    long countByStatus(CertStatus status);
}

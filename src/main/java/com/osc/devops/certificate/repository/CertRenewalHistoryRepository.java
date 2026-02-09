package com.osc.devops.certificate.repository;

import com.osc.devops.certificate.entity.CertRenewalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertRenewalHistoryRepository extends JpaRepository<CertRenewalHistory, Long> {
    List<CertRenewalHistory> findByCertificateIdOrderByRenewedAtDesc(Long certificateId);
}

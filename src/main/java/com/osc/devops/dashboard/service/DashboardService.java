package com.osc.devops.dashboard.service;

import com.osc.devops.addon.service.AddonService;
import com.osc.devops.certificate.repository.CertificateRepository;
import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.common.enums.CertStatus;
import com.osc.devops.customer.repository.CustomerRepository;
import com.osc.devops.dashboard.dto.DashboardDto;
import com.osc.devops.packages.dto.PackageBuildDto;
import com.osc.devops.packages.repository.PackageBuildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final PackageBuildRepository buildRepository;
    private final CertificateRepository certRepository;
    private final AddonService addonService;

    public DashboardDto.Summary getSummary() {
        return DashboardDto.Summary.builder()
                .customerCount(customerRepository.countByIsActiveTrue())
                .addonCount(addonService.countActiveAddons())
                .buildCount(buildRepository.count())
                .successBuildCount(buildRepository.countByStatus(BuildStatus.SUCCESS))
                .activeCertCount(certRepository.countByStatus(CertStatus.ACTIVE))
                .expiringCertCount(certRepository.findByStatusAndExpiresAtBetween(
                        CertStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(30)).size())
                .newVersionCount(addonService.countNewVersions())
                .build();
    }

    public List<PackageBuildDto.Response> getRecentBuilds() {
        return buildRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .getContent().stream()
                .map(PackageBuildDto.Response::from)
                .collect(Collectors.toList());
    }
}

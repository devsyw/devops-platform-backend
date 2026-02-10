package com.osc.devops.certificate.service;

import com.osc.devops.certificate.dto.CertificateDto;
import com.osc.devops.certificate.entity.CertRenewalHistory;
import com.osc.devops.certificate.entity.Certificate;
import com.osc.devops.certificate.repository.CertificateRepository;
import com.osc.devops.common.enums.CertStatus;
import com.osc.devops.common.exception.NotFoundException;
import com.osc.devops.customer.entity.Customer;
import com.osc.devops.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private final CertificateRepository certRepository;
    private final CustomerRepository customerRepository;

    public Page<CertificateDto.Response> getCertificates(Long customerId, Pageable pageable) {
        Page<Certificate> page = (customerId != null)
                ? certRepository.findByCustomerId(customerId, pageable)
                : certRepository.findAll(pageable);
        return page.map(CertificateDto.Response::from);
    }

    public CertificateDto.Response getCertificate(Long id) {
        return CertificateDto.Response.from(findById(id));
    }

    public List<CertificateDto.Response> getExpiringSoon(int days) {
        return certRepository.findByStatusAndExpiresAtBetween(
                CertStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(days))
                .stream().map(CertificateDto.Response::from).collect(Collectors.toList());
    }

    @Transactional
    public CertificateDto.Response create(CertificateDto.CreateRequest req) {
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다."));
        Certificate cert = Certificate.builder()
                .customer(customer).domain(req.getDomain()).issuedAt(req.getIssuedAt())
                .expiresAt(req.getExpiresAt()).issuer(req.getIssuer()).certType(req.getCertType())
                .autoRenew(req.getAutoRenew() != null ? req.getAutoRenew() : false)
                .memo(req.getMemo()).build();
        return CertificateDto.Response.from(certRepository.save(cert));
    }

    @Transactional
    public CertificateDto.Response update(Long id, CertificateDto.UpdateRequest req) {
        Certificate cert = findById(id);
        if (req.getDomain() != null) cert.setDomain(req.getDomain());
        if (req.getIssuedAt() != null) cert.setIssuedAt(req.getIssuedAt());
        if (req.getExpiresAt() != null) cert.setExpiresAt(req.getExpiresAt());
        if (req.getIssuer() != null) cert.setIssuer(req.getIssuer());
        if (req.getCertType() != null) cert.setCertType(req.getCertType());
        if (req.getAutoRenew() != null) cert.setAutoRenew(req.getAutoRenew());
        if (req.getStatus() != null) cert.setStatus(req.getStatus());
        if (req.getMemo() != null) cert.setMemo(req.getMemo());
        return CertificateDto.Response.from(certRepository.save(cert));
    }

    @Transactional
    public CertificateDto.Response renew(Long id, CertificateDto.RenewRequest req) {
        Certificate cert = findById(id);
        CertRenewalHistory history = CertRenewalHistory.builder()
                .certificate(cert).prevExpiresAt(cert.getExpiresAt())
                .newExpiresAt(req.getNewExpiresAt()).renewedAt(LocalDateTime.now())
                .renewedBy(req.getRenewedBy()).memo(req.getMemo()).build();
        cert.getRenewalHistories().add(history);
        cert.setExpiresAt(req.getNewExpiresAt());
        cert.setStatus(CertStatus.ACTIVE);
        return CertificateDto.Response.from(certRepository.save(cert));
    }

    @Transactional
    public void delete(Long id) {
        certRepository.deleteById(id);
    }

    private Certificate findById(Long id) {
        return certRepository.findById(id).orElseThrow(() -> new NotFoundException("인증서를 찾을 수 없습니다."));
    }
}

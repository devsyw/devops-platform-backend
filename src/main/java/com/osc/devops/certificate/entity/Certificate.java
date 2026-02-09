package com.osc.devops.certificate.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import com.osc.devops.common.enums.CertStatus;
import com.osc.devops.common.enums.CertType;
import com.osc.devops.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "certificate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 500)
    private String domain;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(length = 200)
    private String issuer;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_type", length = 50)
    private CertType certType;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private CertStatus status = CertStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "certificate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CertRenewalHistory> renewalHistories = new ArrayList<>();
}

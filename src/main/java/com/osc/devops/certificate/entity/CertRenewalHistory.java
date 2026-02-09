package com.osc.devops.certificate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cert_renewal_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertRenewalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Column(name = "prev_expires_at", nullable = false)
    private LocalDate prevExpiresAt;

    @Column(name = "new_expires_at", nullable = false)
    private LocalDate newExpiresAt;

    @Column(name = "renewed_at", nullable = false)
    private LocalDateTime renewedAt;

    @Column(name = "renewed_by", length = 100)
    private String renewedBy;

    @Column(columnDefinition = "TEXT")
    private String memo;
}

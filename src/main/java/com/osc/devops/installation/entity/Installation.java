package com.osc.devops.installation.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import com.osc.devops.common.enums.InstallStatus;
import com.osc.devops.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "installation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Installation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "installed_at")
    private LocalDateTime installedAt;

    @Column(name = "installed_by", length = 100)
    private String installedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private InstallStatus status = InstallStatus.PLANNED;

    @Column(name = "package_hash", length = 100)
    private String packageHash;

    @Column(name = "package_url", length = 500)
    private String packageUrl;

    @Column(name = "keycloak_enabled", nullable = false)
    @Builder.Default
    private Boolean keycloakEnabled = false;

    @Column(name = "tls_enabled", nullable = false)
    @Builder.Default
    private Boolean tlsEnabled = false;

    @Column(length = 200)
    private String namespace;

    @Column(length = 500)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "installation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InstallAddon> installAddons = new ArrayList<>();
}

package com.osc.devops.packages.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.customer.entity.Customer;
import com.osc.devops.customer.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "package_build")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageBuild extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "build_hash", nullable = false, unique = true, length = 100)
    private String buildHash;

    // 선택된 애드온 + 버전 (JSON)
    // [{"addonId":1,"addonName":"keycloak","version":"26.0.7"}, ...]
    @Column(name = "selected_addons", nullable = false, columnDefinition = "TEXT")
    private String selectedAddons;

    @Column(name = "total_size")
    private Long totalSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BuildStatus status = BuildStatus.BUILDING;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "built_by", length = 100)
    private String builtBy;

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

    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

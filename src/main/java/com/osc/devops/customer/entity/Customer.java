package com.osc.devops.customer.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50, unique = true)
    private String code;

    @Column(length = 50)
    private String environment; // PRODUCTION, STAGING, DEVELOPMENT

    @Column(name = "k8s_version", length = 50)
    private String k8sVersion;

    @Column(name = "os_info", length = 200)
    private String osInfo;

    @Column(name = "node_count")
    private Integer nodeCount;

    @Column(name = "storage_info", columnDefinition = "TEXT")
    private String storageInfo;

    @Column(name = "network_info", columnDefinition = "TEXT")
    private String networkInfo;

    @Column(name = "vpn_info", columnDefinition = "TEXT")
    private String vpnInfo;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

package com.osc.devops.addon.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import com.osc.devops.common.enums.AddonCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "addon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Addon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name; // 시스템 식별 이름 (예: keycloak, jenkins)

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName; // UI 표시 이름 (예: Keycloak, Jenkins)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AddonCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    // Upstream 이미지 목록 (JSON 배열)
    // 예: ["quay.io/keycloak/keycloak", "quay.io/keycloak/keycloak-operator"]
    @Column(name = "upstream_images", columnDefinition = "TEXT")
    private String upstreamImages;

    @Column(name = "helm_repo_url", length = 500)
    private String helmRepoUrl;

    @Column(name = "helm_chart_name", length = 200)
    private String helmChartName;

    // Keycloak OIDC 연동 가능 여부
    @Column(name = "keycloak_enabled", nullable = false)
    @Builder.Default
    private Boolean keycloakEnabled = false;

    // Keycloak Client Config 템플릿 (JSON)
    @Column(name = "keycloak_client_template", columnDefinition = "TEXT")
    private String keycloakClientTemplate;

    // Keycloak Values Override 템플릿 (YAML)
    @Column(name = "keycloak_values_template", columnDefinition = "TEXT")
    private String keycloakValuesTemplate;

    // 설치 우선순위 (낮을수록 먼저 설치)
    @Column(name = "install_order", nullable = false)
    @Builder.Default
    private Integer installOrder = 50;

    // 의존 애드온 이름 목록 (JSON 배열)
    // 예: ["cert-manager"]
    @Column(columnDefinition = "TEXT")
    private String dependencies;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 버전 목록
    @OneToMany(mappedBy = "addon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AddonVersion> versions = new ArrayList<>();
}

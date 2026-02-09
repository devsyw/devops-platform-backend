package com.osc.devops.addon.entity;

import com.osc.devops.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "addon_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddonVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id", nullable = false)
    private Addon addon;

    @Column(nullable = false, length = 100)
    private String version;

    // 이미지 태그 매핑 (JSON)
    // 예: {"keycloak": "26.0.7"}
    @Column(name = "image_tags", columnDefinition = "TEXT")
    private String imageTags;

    @Column(name = "helm_chart_version", length = 100)
    private String helmChartVersion;

    @Column(name = "is_latest", nullable = false)
    @Builder.Default
    private Boolean isLatest = false;

    @Column(name = "release_note_url", length = 500)
    private String releaseNoteUrl;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}

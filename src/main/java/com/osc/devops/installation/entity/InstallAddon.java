package com.osc.devops.installation.entity;

import com.osc.devops.addon.entity.Addon;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "install_addon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id", nullable = false)
    private Installation installation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id", nullable = false)
    private Addon addon;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(name = "helm_chart_version", length = 100)
    private String helmChartVersion;
}

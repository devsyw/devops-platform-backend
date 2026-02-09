-- ============================================================
-- V2: 초기 애드온 12종 시딩
-- ============================================================

-- 1. Cert-Manager (인프라, 설치순서 10)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('cert-manager', 'Cert-Manager', 'INFRA', 'X.509 인증서 관리자. TLS 인증서 자동 발급 및 갱신', '/icons/cert-manager.svg',
 '["quay.io/jetstack/cert-manager-controller","quay.io/jetstack/cert-manager-webhook","quay.io/jetstack/cert-manager-cainjector","quay.io/jetstack/cert-manager-startupapicheck"]',
 'https://charts.jetstack.io', 'cert-manager', FALSE, 10, '[]');

-- 2. Keycloak (보안, 설치순서 20)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('keycloak', 'Keycloak', 'SECURITY', 'SSO 및 ID 관리 솔루션. OIDC/SAML 지원', '/icons/keycloak.svg',
 '["quay.io/keycloak/keycloak"]',
 'https://codecentric.github.io/helm-charts', 'keycloakx', FALSE, 20, '["cert-manager"]');

-- 3. Harbor (소스관리, 설치순서 30)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('harbor', 'Harbor', 'SOURCE', '컨테이너 이미지 레지스트리. 보안 스캔, 이미지 서명 지원', '/icons/harbor.svg',
 '["goharbor/harbor-core","goharbor/harbor-portal","goharbor/harbor-jobservice","goharbor/harbor-registry","goharbor/harbor-registryctl","goharbor/harbor-db","goharbor/redis-photon","goharbor/trivy-adapter-photon"]',
 'https://helm.goharbor.io', 'harbor', TRUE, 30, '["cert-manager"]');

-- 4. Gitea (소스관리, 설치순서 30)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('gitea', 'Gitea', 'SOURCE', '경량 Git 호스팅 서비스. GitHub 대안', '/icons/gitea.svg',
 '["gitea/gitea"]',
 'https://dl.gitea.io/charts/', 'gitea', TRUE, 30, '[]');

-- 5. GitLab (소스관리, 설치순서 30)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('gitlab', 'GitLab', 'SOURCE', '통합 DevOps 플랫폼. Git, CI/CD, 이슈 관리', '/icons/gitlab.svg',
 '["gitlab/gitlab-ce"]',
 'https://charts.gitlab.io/', 'gitlab', TRUE, 30, '["cert-manager"]');

-- 6. Jenkins (CI/CD, 설치순서 40)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('jenkins', 'Jenkins', 'CI_CD', '오픈소스 CI/CD 자동화 서버', '/icons/jenkins.svg',
 '["jenkins/jenkins"]',
 'https://charts.jenkins.io', 'jenkins', TRUE, 40, '[]');

-- 7. ArgoCD (CI/CD, 설치순서 40)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('argocd', 'ArgoCD', 'CI_CD', 'GitOps 기반 Kubernetes CD 도구', '/icons/argocd.svg',
 '["quay.io/argoproj/argocd","redis"]',
 'https://argoproj.github.io/argo-helm', 'argo-cd', TRUE, 40, '[]');

-- 8. SonarQube (품질관리, 설치순서 50)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('sonarqube', 'SonarQube', 'QUALITY', '코드 품질 및 보안 분석 도구', '/icons/sonarqube.svg',
 '["sonarqube"]',
 'https://SonarSource.github.io/helm-chart-sonarqube', 'sonarqube', TRUE, 50, '[]');

-- 9. Nexus (아티팩트, 설치순서 50)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('nexus', 'Nexus', 'ARTIFACT', '아티팩트 저장소. Maven, npm, Docker Registry 지원', '/icons/nexus.svg',
 '["sonatype/nexus3"]',
 'https://sonatype.github.io/helm3-charts/', 'nexus-repository-manager', TRUE, 50, '[]');

-- 10. Vault (보안, 설치순서 50)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('vault', 'Vault', 'SECURITY', '시크릿 관리 및 데이터 암호화 도구', '/icons/vault.svg',
 '["hashicorp/vault"]',
 'https://helm.releases.hashicorp.com', 'vault', TRUE, 50, '[]');

-- 11. 모니터링 스택 (모니터링, 설치순서 60)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('monitoring', 'Monitoring Stack', 'MONITORING', 'Prometheus + Grafana + Loki 모니터링 스택', '/icons/grafana.svg',
 '["grafana/grafana","grafana/loki","prom/prometheus","prom/alertmanager","jimmidyson/configmap-reload"]',
 'https://prometheus-community.github.io/helm-charts', 'kube-prometheus-stack', TRUE, 60, '[]');

-- 12. Service Mesh (네트워크, 설치순서 60)
INSERT INTO addon (name, display_name, category, description, icon_url, upstream_images, helm_repo_url, helm_chart_name, keycloak_enabled, install_order, dependencies) VALUES
('service-mesh', 'Service Mesh (Istio)', 'NETWORK', '서비스 메시. 트래픽 관리, mTLS, 관측성', '/icons/istio.svg',
 '["istio/pilot","istio/proxyv2"]',
 'https://istio-release.storage.googleapis.com/charts', 'istiod', FALSE, 60, '[]');

-- 초기 버전 시딩 (각 애드온별 최신 버전 1개씩)
INSERT INTO addon_version (addon_id, version, image_tags, helm_chart_version, is_latest, synced_at) VALUES
(1, '1.14.5', '{"controller":"v1.14.5","webhook":"v1.14.5","cainjector":"v1.14.5"}', '1.14.5', TRUE, CURRENT_TIMESTAMP),
(2, '26.0.7', '{"keycloak":"26.0.7"}', '2.4.0', TRUE, CURRENT_TIMESTAMP),
(3, '2.11.0', '{"core":"v2.11.0","portal":"v2.11.0","registry":"v2.11.0"}', '1.15.0', TRUE, CURRENT_TIMESTAMP),
(4, '1.22.0', '{"gitea":"1.22.0"}', '10.4.0', TRUE, CURRENT_TIMESTAMP),
(5, '17.5.0', '{"gitlab-ce":"17.5.0-ce.0"}', '8.5.0', TRUE, CURRENT_TIMESTAMP),
(6, '2.479', '{"jenkins":"2.479-jdk17"}', '5.5.0', TRUE, CURRENT_TIMESTAMP),
(7, '2.13.2', '{"argocd":"v2.13.2"}', '7.7.0', TRUE, CURRENT_TIMESTAMP),
(8, '10.7.0', '{"sonarqube":"10.7.0-community"}', '10.7.0', TRUE, CURRENT_TIMESTAMP),
(9, '3.72.0', '{"nexus3":"3.72.0"}', '72.0.0', TRUE, CURRENT_TIMESTAMP),
(10, '1.17.3', '{"vault":"1.17.3"}', '0.28.0', TRUE, CURRENT_TIMESTAMP),
(11, '11.3.0', '{"grafana":"11.3.0","loki":"3.2.0","prometheus":"2.54.0"}', '65.0.0', TRUE, CURRENT_TIMESTAMP),
(12, '1.23.0', '{"pilot":"1.23.0","proxyv2":"1.23.0"}', '1.23.0', TRUE, CURRENT_TIMESTAMP);

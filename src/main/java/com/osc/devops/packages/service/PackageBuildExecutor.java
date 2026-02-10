package com.osc.devops.packages.service;

import com.osc.devops.common.enums.BuildStatus;
import com.osc.devops.packages.dto.PackageBuildDto;
import com.osc.devops.packages.entity.PackageBuild;
import com.osc.devops.packages.repository.PackageBuildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageBuildExecutor {

    private final PackageBuildRepository buildRepository;

    @Value("${package.build.storage-path:/tmp/devops-platform/packages}")
    private String storagePath;

    @Async
    public void executeBuild(Long buildId, List<Map<String, Object>> addonInfoList,
                             PackageBuildDto.BuildRequest request) {
        log.info("패키지 빌드 시작: buildId={}, deployEnv={}", buildId, request.getDeployEnv());
        Path buildDir = null;

        try {
            PackageBuild build = buildRepository.findById(buildId)
                    .orElseThrow(() -> new RuntimeException("빌드 레코드를 찾을 수 없습니다. id=" + buildId));

            // 빌드 디렉토리 생성
            buildDir = Paths.get(storagePath, build.getBuildHash());
            Files.createDirectories(buildDir);
            updateProgress(build, 5);

            // deploy.sh 생성
            generateDeployScript(buildDir, addonInfoList, request);
            updateProgress(build, 15);

            // Helm values 생성
            generateHelmValues(buildDir, addonInfoList, request);
            updateProgress(build, 25);

            // 이미지 목록 생성
            generateImageList(buildDir, addonInfoList);
            updateProgress(build, 30);

            // Keycloak 설정
            if (request.isKeycloakEnabled()) {
                generateKeycloakConfig(buildDir, addonInfoList, request);
            }
            updateProgress(build, 35);

            // ============ 폐쇄망: helm chart pull + docker image pull/save ============
            if (request.isAirgapped()) {
                log.info("폐쇄망 빌드 모드 - helm chart pull 시작");
                pullHelmCharts(buildDir, addonInfoList, build);
                updateProgress(build, 55);

                log.info("폐쇄망 빌드 모드 - docker image pull/save 시작");
                pullAndSaveImages(buildDir, addonInfoList, request, build);
                updateProgress(build, 80);

                // push-to-registry.sh 생성
                generatePushToRegistryScript(buildDir, addonInfoList, request);
            }
            updateProgress(build, 82);

            // install.sh
            generateInstallScript(buildDir, addonInfoList, request);
            updateProgress(build, 85);

            // README
            generateReadme(buildDir, addonInfoList, request);
            updateProgress(build, 88);

            // tar.gz 패키징 (Java 내장)
            String tarFileName = build.getBuildHash() + ".tar.gz";
            Path tarPath = Paths.get(storagePath, tarFileName);
            createTarGzJava(buildDir, tarPath, build.getBuildHash());
            updateProgress(build, 95);

            long totalSize = Files.size(tarPath);

            build.setStatus(BuildStatus.SUCCESS);
            build.setFilePath(tarPath.toString());
            build.setTotalSize(totalSize);
            build.setProgress(100);
            buildRepository.save(build);

            log.info("패키지 빌드 완료: hash={}, size={}MB, files={}, airgapped={}",
                    build.getBuildHash(), totalSize / 1024 / 1024, countFiles(buildDir), request.isAirgapped());

        } catch (Exception e) {
            log.error("패키지 빌드 실패: buildId={}", buildId, e);
            buildRepository.findById(buildId).ifPresent(b -> {
                b.setStatus(BuildStatus.FAILED);
                b.setProgress(-1);
                buildRepository.save(b);
            });
        } finally {
            // 빌드 디렉토리 정리 (tar.gz만 남기고)
            if (buildDir != null) {
                try { deleteDirectory(buildDir); } catch (Exception ignored) {}
            }
        }
    }

    // ======================== tar.gz (Java 내장, 외부 프로세스 불필요) ========================

    private void createTarGzJava(Path sourceDir, Path tarPath, String rootDirName) throws IOException {
        try (OutputStream fos = Files.newOutputStream(tarPath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzos)) {

            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            Files.walk(sourceDir).forEach(path -> {
                try {
                    if (path.equals(sourceDir)) return;

                    String entryName = rootDirName + "/" + sourceDir.relativize(path).toString()
                            .replace("\\", "/"); // Windows 호환

                    if (Files.isDirectory(path)) {
                        TarArchiveEntry entry = new TarArchiveEntry(entryName + "/");
                        tar.putArchiveEntry(entry);
                        tar.closeArchiveEntry();
                    } else {
                        TarArchiveEntry entry = new TarArchiveEntry(entryName);
                        entry.setSize(Files.size(path));
                        tar.putArchiveEntry(entry);
                        Files.copy(path, tar);
                        tar.closeArchiveEntry();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            tar.finish();
        }
    }

    // ======================== 파일 생성 (Makefile, values, images, scripts, readme) ========================

    private void generateDeployScript(Path buildDir, List<Map<String, Object>> addons,
                                      PackageBuildDto.BuildRequest request) throws IOException {
        String ns = sanitizeNamespace(request.getNamespace());
        String domain = sanitizeDomain(request.getDomain());

        List<Map<String, Object>> sorted = addons.stream()
                .sorted(Comparator.comparingInt(a -> (Integer) a.getOrDefault("installOrder", 50)))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");
        sb.append("SCRIPT_DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n\n");

        sb.append("# ============================================================\n");
        sb.append("# DevOps Platform - 애드온 배포 스크립트\n");
        sb.append("# 생성: ").append(LocalDateTime.now().toLocalDate()).append("\n");
        sb.append("# 모드: ").append(request.isAirgapped() ? "폐쇄망 (로컬 chart + image)" : "인터넷").append("\n");
        sb.append("# ============================================================\n\n");

        // 설정 변수
        sb.append("NAMESPACE=\"${NAMESPACE:-").append(ns).append("}\"\n");
        sb.append("DOMAIN=\"${DOMAIN:-").append(domain).append("}\"\n");
        sb.append("REGISTRY=\"harbor.${DOMAIN}\"\n");
        sb.append("TLS_ENABLED=\"").append(request.isTlsEnabled()).append("\"\n");
        sb.append("KEYCLOAK_ENABLED=\"").append(request.isKeycloakEnabled()).append("\"\n");
        sb.append("TIMEOUT=\"600s\"\n\n");

        // 색상
        sb.append("RED='\\033[0;31m'; GREEN='\\033[0;32m'; YELLOW='\\033[1;33m'; NC='\\033[0m'\n\n");

        // pre_check
        sb.append("pre_check() {\n");
        sb.append("  echo \"🔍 사전 점검...\"\n");
        sb.append("  command -v helm >/dev/null 2>&1 || { echo -e \"${RED}❌ helm이 설치되어 있지 않습니다${NC}\"; exit 1; }\n");
        sb.append("  command -v kubectl >/dev/null 2>&1 || { echo -e \"${RED}❌ kubectl이 설치되어 있지 않습니다${NC}\"; exit 1; }\n");
        sb.append("  kubectl create namespace \"$NAMESPACE\" --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null\n");
        sb.append("  echo -e \"${GREEN}✅ 사전 점검 완료 (namespace: $NAMESPACE)${NC}\"\n");
        sb.append("}\n\n");

        // load_images
        sb.append("load_images() {\n");
        sb.append("  echo \"📦 이미지 로드 시작...\"\n");
        sb.append("  if [ ! -d \"$SCRIPT_DIR/images\" ] || [ -z \"$(ls -A $SCRIPT_DIR/images/*.tar 2>/dev/null)\" ]; then\n");
        sb.append("    echo -e \"${YELLOW}⚠️  images/ 디렉토리에 tar 파일이 없습니다${NC}\"\n");
        sb.append("    return 0\n");
        sb.append("  fi\n");
        sb.append("  for img in $SCRIPT_DIR/images/*.tar; do\n");
        sb.append("    echo \"  로드: $(basename $img)\"\n");
        sb.append("    docker load -i \"$img\"\n");
        sb.append("  done\n");
        sb.append("  echo -e \"${GREEN}✅ 이미지 로드 완료${NC}\"\n");
        sb.append("}\n\n");

        // 개별 install/uninstall 함수
        boolean airgapped = request.isAirgapped();
        for (Map<String, Object> a : addons) {
            String name = (String) a.get("name");
            String displayName = (String) a.get("displayName");
            String helmRepo = (String) a.get("helmRepoUrl");
            String helmChartName = (String) a.get("helmChartName");
            String chartVersion = (String) a.get("helmChartVersion");

            // chart 참조 결정
            String chartRef;
            if (airgapped) {
                // 폐쇄망: 로컬 tgz (helm pull 결과)
                String chartFileName = (helmChartName != null && !helmChartName.isEmpty() ? helmChartName : name);
                if (chartVersion != null && !chartVersion.isEmpty()) {
                    chartRef = "$SCRIPT_DIR/charts/" + chartFileName + "-" + chartVersion + ".tgz";
                } else {
                    // 버전 모르면 glob
                    chartRef = "$(ls $SCRIPT_DIR/charts/" + chartFileName + "-*.tgz 2>/dev/null | head -1)";
                }
            } else {
                // 인터넷: repo/chart
                chartRef = (helmChartName != null && !helmChartName.isEmpty())
                        ? name + "/" + helmChartName : name + "/" + name;
            }
            String funcName = name.replace("-", "_");

            sb.append("install_").append(funcName).append("() {\n");
            sb.append("  echo \"📦 ").append(displayName).append(" 설치 시작...\"\n");
            if (!airgapped && helmRepo != null && !helmRepo.isEmpty()) {
                sb.append("  helm repo add ").append(name).append(" ").append(helmRepo).append(" 2>/dev/null || true\n");
                sb.append("  helm repo update ").append(name).append(" 2>/dev/null || true\n");
            }
            sb.append("  local VALUES=\"-f $SCRIPT_DIR/values/").append(name).append(".yaml\"\n");
            if (request.isTlsEnabled()) {
                sb.append("  [ -f \"$SCRIPT_DIR/values/").append(name).append("-tls.yaml\" ] && VALUES=\"$VALUES -f $SCRIPT_DIR/values/").append(name).append("-tls.yaml\"\n");
            }
            if (request.isKeycloakEnabled() && Boolean.TRUE.equals(a.get("keycloakEnabled"))) {
                sb.append("  [ -f \"$SCRIPT_DIR/values/").append(name).append("-keycloak.yaml\" ] && VALUES=\"$VALUES -f $SCRIPT_DIR/values/").append(name).append("-keycloak.yaml\"\n");
            }
            sb.append("  helm upgrade --install ").append(name).append(" ").append(chartRef);
            sb.append(" -n \"$NAMESPACE\" --create-namespace");
            sb.append(" $VALUES");
            if (chartVersion != null && !chartVersion.isEmpty()) sb.append(" --version ").append(chartVersion);
            sb.append(" --wait --timeout \"$TIMEOUT\"\n");
            sb.append("  echo -e \"${GREEN}  ✅ ").append(displayName).append(" 설치 완료${NC}\"\n");
            sb.append("}\n\n");

            sb.append("uninstall_").append(funcName).append("() {\n");
            sb.append("  echo \"🗑️  ").append(displayName).append(" 삭제...\"\n");
            sb.append("  helm uninstall ").append(name).append(" -n \"$NAMESPACE\" 2>/dev/null || echo \"  (이미 삭제됨)\"\n");
            sb.append("}\n\n");
        }

        // install_all
        sb.append("install_all() {\n");
        sb.append("  pre_check\n");
        if (airgapped) {
            sb.append("  load_images\n");
        }
        sb.append("  echo \"\"\n");
        sb.append("  echo \"========================================\"\n");
        sb.append("  echo \"  전체 설치 시작 (").append(sorted.size()).append("개 애드온)\"\n");
        sb.append("  echo \"  모드: ").append(airgapped ? "폐쇄망 (로컬 chart + image)" : "인터넷").append("\"\n");
        sb.append("  echo \"========================================\"\n");
        sb.append("  echo \"\"\n");
        for (Map<String, Object> a : sorted) {
            sb.append("  install_").append(((String) a.get("name")).replace("-", "_")).append("\n");
        }
        if (request.isKeycloakEnabled()) {
            sb.append("  echo \"\"\n");
            sb.append("  echo \"🔐 Keycloak SSO 설정 시작...\"\n");
            sb.append("  bash \"$SCRIPT_DIR/scripts/configure-keycloak.sh\"\n");
        }
        sb.append("  echo \"\"\n");
        sb.append("  echo -e \"${GREEN}========================================${NC}\"\n");
        sb.append("  echo -e \"${GREEN}  ✅ 전체 설치 완료${NC}\"\n");
        sb.append("  echo -e \"${GREEN}========================================${NC}\"\n");
        sb.append("}\n\n");

        // uninstall_all (역순)
        sb.append("uninstall_all() {\n");
        sb.append("  echo \"========================================\"\n");
        sb.append("  echo \"  전체 삭제 시작\"\n");
        sb.append("  echo \"========================================\"\n");
        List<Map<String, Object>> reversed = new ArrayList<>(sorted);
        Collections.reverse(reversed);
        for (Map<String, Object> a : reversed) {
            sb.append("  uninstall_").append(((String) a.get("name")).replace("-", "_")).append("\n");
        }
        sb.append("  echo -e \"${GREEN}✅ 전체 삭제 완료${NC}\"\n");
        sb.append("}\n\n");

        // status
        sb.append("status() {\n");
        sb.append("  echo \"========================================\"\n");
        sb.append("  echo \"  배포 상태 (namespace: $NAMESPACE)\"\n");
        sb.append("  echo \"========================================\"\n");
        sb.append("  helm list -n \"$NAMESPACE\" 2>/dev/null || echo \"배포된 릴리즈 없음\"\n");
        sb.append("  echo \"\"\n");
        sb.append("  kubectl get pods -n \"$NAMESPACE\" 2>/dev/null || true\n");
        sb.append("}\n\n");

        // usage
        sb.append("usage() {\n");
        sb.append("  echo \"사용법: $0 <command> [addon]\"\n");
        sb.append("  echo \"\"\n");
        sb.append("  echo \"Commands:\"\n");
        sb.append("  echo \"  install-all       전체 설치 (의존성 순서)\"\n");
        sb.append("  echo \"  uninstall-all     전체 삭제 (역순)\"\n");
        sb.append("  echo \"  install <addon>   개별 애드온 설치\"\n");
        sb.append("  echo \"  uninstall <addon> 개별 애드온 삭제\"\n");
        sb.append("  echo \"  load-images       폐쇄망 이미지 로드\"\n");
        sb.append("  echo \"  status            배포 상태 확인\"\n");
        sb.append("  echo \"\"\n");
        sb.append("  echo \"Addons:\"\n");
        for (Map<String, Object> a : sorted) {
            sb.append("  echo \"  ").append(String.format("%-20s", a.get("name"))).append(a.get("displayName")).append("\"\n");
        }
        sb.append("  echo \"\"\n");
        sb.append("  echo \"Environment:\"\n");
        sb.append("  echo \"  NAMESPACE=").append(ns).append("  DOMAIN=").append(domain).append("\"\n");
        sb.append("  echo \"\"\n");
        sb.append("  echo \"Examples:\"\n");
        sb.append("  echo \"  $0 install-all\"\n");
        sb.append("  echo \"  $0 install keycloak\"\n");
        sb.append("  echo \"  $0 uninstall harbor\"\n");
        sb.append("  echo \"  NAMESPACE=prod DOMAIN=prod.com $0 install-all\"\n");
        sb.append("}\n\n");

        // main dispatcher
        sb.append("# ============================================================\n");
        sb.append("# 메인\n");
        sb.append("# ============================================================\n");
        sb.append("case \"${1:-}\" in\n");
        sb.append("  install-all)   install_all ;;\n");
        sb.append("  uninstall-all) uninstall_all ;;\n");
        sb.append("  load-images)   load_images ;;\n");
        sb.append("  status)        status ;;\n");
        sb.append("  install)\n");
        sb.append("    [ -z \"${2:-}\" ] && { echo \"사용법: $0 install <addon>\"; exit 1; }\n");
        sb.append("    FUNC=\"install_$(echo $2 | tr '-' '_')\"\n");
        sb.append("    if type \"$FUNC\" &>/dev/null; then pre_check; $FUNC\n");
        sb.append("    else echo -e \"${RED}❌ 알 수 없는 애드온: $2${NC}\"; exit 1; fi\n");
        sb.append("    ;;\n");
        sb.append("  uninstall)\n");
        sb.append("    [ -z \"${2:-}\" ] && { echo \"사용법: $0 uninstall <addon>\"; exit 1; }\n");
        sb.append("    FUNC=\"uninstall_$(echo $2 | tr '-' '_')\"\n");
        sb.append("    if type \"$FUNC\" &>/dev/null; then $FUNC\n");
        sb.append("    else echo -e \"${RED}❌ 알 수 없는 애드온: $2${NC}\"; exit 1; fi\n");
        sb.append("    ;;\n");
        sb.append("  *) usage ;;\n");
        sb.append("esac\n");

        writeFile(buildDir.resolve("deploy.sh"), sb.toString());
    }

    private void generateHelmValues(Path buildDir, List<Map<String, Object>> addons,
                                    PackageBuildDto.BuildRequest request) throws IOException {
        Path valuesDir = buildDir.resolve("values");
        Files.createDirectories(valuesDir);

        String ns = sanitizeNamespace(request.getNamespace());
        String domain = sanitizeDomain(request.getDomain());

        for (Map<String, Object> addon : addons) {
            String name = (String) addon.get("name");
            String version = (String) addon.get("version");

            // values 파일
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(addon.get("displayName")).append(" v").append(version).append("\n\n");

            switch (name) {
                case "cert-manager" -> sb.append("installCRDs: true\nreplicaCount: 1\n");
                case "keycloak" -> sb.append("replicas: 1\nextraEnv: |\n  - name: KEYCLOAK_ADMIN\n    value: admin\n  - name: KEYCLOAK_ADMIN_PASSWORD\n    value: admin123\ningress:\n  enabled: true\n  rules:\n    - host: keycloak.").append(domain).append("\n      paths:\n        - path: /\n          pathType: Prefix\n");
                case "harbor" -> sb.append("expose:\n  type: ingress\n  ingress:\n    hosts:\n      core: harbor.").append(domain).append("\nexternalURL: https://harbor.").append(domain).append("\npersistence:\n  enabled: true\n");
                case "gitea" -> sb.append("gitea:\n  admin:\n    username: gitea_admin\n    password: admin123\ningress:\n  enabled: true\n  hosts:\n    - host: gitea.").append(domain).append("\n");
                case "gitlab" -> sb.append("global:\n  hosts:\n    domain: ").append(domain).append("\n    gitlab:\n      name: gitlab.").append(domain).append("\n");
                case "jenkins" -> sb.append("controller:\n  adminUser: admin\n  adminPassword: admin123\n  ingress:\n    enabled: true\n    hostName: jenkins.").append(domain).append("\n");
                case "argocd" -> sb.append("server:\n  ingress:\n    enabled: true\n    hosts:\n      - argocd.").append(domain).append("\nconfigs:\n  params:\n    server.insecure: true\n");
                case "sonarqube" -> sb.append("ingress:\n  enabled: true\n  hosts:\n    - name: sonarqube.").append(domain).append("\n");
                case "nexus" -> sb.append("ingress:\n  enabled: true\n  hostRepo: nexus.").append(domain).append("\n");
                case "vault" -> sb.append("server:\n  ingress:\n    enabled: true\n    hosts:\n      - host: vault.").append(domain).append("\n");
                case "monitoring" -> sb.append("grafana:\n  adminPassword: admin123\n  ingress:\n    enabled: true\n    hosts:\n      - grafana.").append(domain).append("\nprometheus:\n  prometheusSpec:\n    retention: 15d\n");
                case "service-mesh" -> sb.append("pilot:\n  resources:\n    requests:\n      cpu: 100m\n      memory: 128Mi\n");
                default -> sb.append("# 커스텀 설정\n");
            }

            writeFile(valuesDir.resolve(name + ".yaml"), sb.toString());

            if (request.isTlsEnabled()) {
                writeFile(valuesDir.resolve(name + "-tls.yaml"), generateTlsValues(name, domain));
            }
            if (request.isKeycloakEnabled() && Boolean.TRUE.equals(addon.get("keycloakEnabled"))) {
                writeFile(valuesDir.resolve(name + "-keycloak.yaml"), generateKeycloakValues(name, domain));
            }
        }
    }

    private String generateTlsValues(String name, String domain) {
        String host = name + "." + domain;
        String secretName = name + "-tls";
        String issuer = "letsencrypt-prod"; // ClusterIssuer 이름

        return switch (name) {
            case "keycloak" -> """
                    ingress:
                      annotations:
                        cert-manager.io/cluster-issuer: %s
                      tls:
                        - secretName: %s
                          hosts:
                            - %s
                    """.formatted(issuer, secretName, host);
            case "harbor" -> """
                    expose:
                      tls:
                        enabled: true
                        certSource: secret
                        secret:
                          secretName: %s
                      ingress:
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                    """.formatted(secretName, issuer);
            case "gitea" -> """
                    ingress:
                      annotations:
                        cert-manager.io/cluster-issuer: %s
                      tls:
                        - secretName: %s
                          hosts:
                            - %s
                    """.formatted(issuer, secretName, host);
            case "gitlab" -> """
                    global:
                      ingress:
                        configureCertmanager: true
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                        tls:
                          - secretName: %s
                            hosts:
                              - gitlab.%s
                    """.formatted(issuer, secretName, domain);
            case "jenkins" -> """
                    controller:
                      ingress:
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                        tls:
                          - secretName: %s
                            hosts:
                              - %s
                    """.formatted(issuer, secretName, host);
            case "argocd" -> """
                    server:
                      ingress:
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                        tls:
                          - secretName: %s
                            hosts:
                              - %s
                    configs:
                      params:
                        server.insecure: false
                    """.formatted(issuer, secretName, host);
            case "sonarqube" -> """
                    ingress:
                      annotations:
                        cert-manager.io/cluster-issuer: %s
                      tls:
                        - secretName: %s
                          hosts:
                            - %s
                    """.formatted(issuer, secretName, host);
            case "nexus" -> """
                    ingress:
                      annotations:
                        cert-manager.io/cluster-issuer: %s
                      tls:
                        - secretName: %s
                          hosts:
                            - %s
                    """.formatted(issuer, secretName, host);
            case "vault" -> """
                    server:
                      ingress:
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                        tls:
                          - secretName: %s
                            hosts:
                              - %s
                    """.formatted(issuer, secretName, host);
            case "monitoring" -> """
                    grafana:
                      ingress:
                        annotations:
                          cert-manager.io/cluster-issuer: %s
                        tls:
                          - secretName: grafana-tls
                            hosts:
                              - grafana.%s
                    """.formatted(issuer, domain);
            default -> """
                    # %s TLS 설정
                    # cert-manager ClusterIssuer: %s
                    """.formatted(name, issuer);
        };
    }

    private String generateKeycloakValues(String name, String domain) {
        String kcUrl = "https://keycloak." + domain;
        String realm = "devops";
        String clientId = name;

        return switch (name) {
            case "harbor" -> """
                    # Harbor OIDC 연동 (Keycloak)
                    # Harbor는 helm values가 아닌 관리자 UI 또는 harbor.yml에서 설정
                    # 설치 후 Harbor UI > Configuration > Authentication 에서 설정:
                    #   Auth Mode: OIDC
                    #   OIDC Provider: Keycloak
                    #   OIDC Endpoint: %s/realms/%s
                    #   OIDC Client ID: %s
                    #   OIDC Client Secret: (configure-keycloak.sh 실행 후 생성됨)
                    #   OIDC Scope: openid,profile,email
                    """.formatted(kcUrl, realm, clientId);
            case "gitea" -> """
                    gitea:
                      oauth:
                        - name: keycloak
                          provider: openidConnect
                          clientID: %s
                          clientSecret: changeme-run-configure-keycloak-sh
                          autoDiscoverUrl: %s/realms/%s/.well-known/openid-configuration
                          scopes: openid profile email
                    """.formatted(clientId, kcUrl, realm);
            case "jenkins" -> """
                    controller:
                      JCasC:
                        securityRealm: |-
                          oic:
                            clientId: %s
                            clientSecret: changeme-run-configure-keycloak-sh
                            wellKnownOpenIDConfigurationUrl: %s/realms/%s/.well-known/openid-configuration
                            userNameField: preferred_username
                            fullNameFieldName: name
                            emailFieldName: email
                            scopes: openid profile email
                            logoutFromOpenidProvider: true
                            endSessionEndpoint: %s/realms/%s/protocol/openid-connect/logout
                      installPlugins:
                        - oic-auth:latest
                    """.formatted(clientId, kcUrl, realm, kcUrl, realm);
            case "argocd" -> """
                    configs:
                      cm:
                        url: https://argocd.%s
                        oidc.config: |
                          name: Keycloak
                          issuer: %s/realms/%s
                          clientID: %s
                          clientSecret: changeme-run-configure-keycloak-sh
                          requestedScopes:
                            - openid
                            - profile
                            - email
                      rbac:
                        policy.csv: |
                          g, /devops-admin, role:admin
                    """.formatted(domain, kcUrl, realm, clientId);
            case "sonarqube" -> """
                    sonarProperties:
                      sonar.auth.oidc.enabled: "true"
                      sonar.auth.oidc.issuerUri: %s/realms/%s
                      sonar.auth.oidc.clientId.secured: %s
                      sonar.auth.oidc.clientSecret.secured: changeme-run-configure-keycloak-sh
                      sonar.auth.oidc.scopes: openid profile email
                    plugins:
                      install:
                        - https://github.com/vaulttec/sonar-auth-oidc/releases/download/v2.1.1/sonar-auth-oidc-plugin-2.1.1.jar
                    """.formatted(kcUrl, realm, clientId);
            case "nexus" -> """
                    # Nexus OIDC: Keycloak 연동은 Nexus Pro 전용 기능
                    # Community 버전은 SAML/OIDC 미지원
                    # 대안: keycloak-proxy (oauth2-proxy) 사이드카 사용
                    nexus:
                      env:
                        - name: NEXUS_SECURITY_INITIAL_PASSWORD
                          value: admin123
                    """;
            default -> """
                    # %s Keycloak OIDC 연동
                    # OIDC Endpoint: %s/realms/%s
                    # Client ID: %s
                    """.formatted(name, kcUrl, realm, clientId);
        };
    }

    private void generateImageList(Path buildDir, List<Map<String, Object>> addons) throws IOException {
        Path imagesDir = buildDir.resolve("images");
        Files.createDirectories(imagesDir);
        writeFile(imagesDir.resolve(".gitkeep"), "");

        StringBuilder sb = new StringBuilder();
        sb.append("# 필요 이미지 목록 (폐쇄망 배포 시 Harbor 미러링 필요)\n\n");
        for (Map<String, Object> a : addons) {
            sb.append("# ").append(a.get("displayName")).append("\n");
            String images = (String) a.get("upstreamImages");
            if (images != null && !images.isEmpty()) {
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> list = mapper.readValue(images, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                    String version = (String) a.get("version");
                    for (String img : list) {
                        // 공식 이미지(슬래시 없음: redis, sonarqube 등)는 addon 버전과 다를 수 있으므로 latest
                        if (!img.contains("/")) {
                            sb.append(img).append(":latest\n");
                        } else {
                            sb.append(img).append(":").append(version).append("\n");
                        }
                    }
                } catch (Exception ignored) {}
            }
            sb.append("\n");
        }
        writeFile(buildDir.resolve("images.txt"), sb.toString());
    }


    private void generateKeycloakConfig(Path buildDir, List<Map<String, Object>> addons,
                                        PackageBuildDto.BuildRequest request) throws IOException {
        Path scriptsDir = buildDir.resolve("scripts");
        Files.createDirectories(scriptsDir);
        String domain = sanitizeDomain(request.getDomain());
        String ns = sanitizeNamespace(request.getNamespace());

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");
        sb.append("SCRIPT_DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n");
        sb.append("BASE_DIR=\"$(dirname \"$SCRIPT_DIR\")\"\n\n");
        sb.append("# ============================================================\n");
        sb.append("# Keycloak SSO 자동 설정\n");
        sb.append("# 1) Realm 생성\n");
        sb.append("# 2) OIDC 클라이언트 등록 + Secret 발급\n");
        sb.append("# 3) values/*-keycloak.yaml 에 Secret 자동 반영\n");
        sb.append("# 4) SSO 대상 애드온 helm upgrade 재배포\n");
        sb.append("# ============================================================\n\n");

        sb.append("KC_URL=\"https://keycloak.").append(domain).append("\"\n");
        sb.append("KC_ADMIN=\"${KC_ADMIN:-admin}\"\n");
        sb.append("KC_PASS=\"${KC_PASS:-admin123}\"\n");
        sb.append("REALM=\"devops\"\n");
        sb.append("NAMESPACE=\"").append(ns).append("\"\n");
        sb.append("PLACEHOLDER=\"changeme-run-configure-keycloak-sh\"\n\n");

        // macOS sed 호환 함수
        sb.append("# macOS/Linux sed 호환 함수\n");
        sb.append("replace_in_file() {\n");
        sb.append("  local file=\"$1\" old=\"$2\" new=\"$3\"\n");
        sb.append("  if [[ \"$OSTYPE\" == \"darwin\"* ]]; then\n");
        sb.append("    sed -i '' \"s|${old}|${new}|g\" \"$file\"\n");
        sb.append("  else\n");
        sb.append("    sed -i \"s|${old}|${new}|g\" \"$file\"\n");
        sb.append("  fi\n");
        sb.append("}\n\n");

        // health check
        sb.append("echo \"🔐 Keycloak 연결 확인: $KC_URL\"\n");
        sb.append("until curl -sf \"$KC_URL/health/ready\" > /dev/null 2>&1; do\n");
        sb.append("  echo \"  대기 중...\"\n  sleep 5\ndone\n");
        sb.append("echo \"✅ Keycloak 준비됨\"\n\n");

        // 토큰
        sb.append("# 토큰 발급\n");
        sb.append("TOKEN=$(curl -sf -X POST \"$KC_URL/realms/master/protocol/openid-connect/token\" \\\n");
        sb.append("  -d \"client_id=admin-cli\" -d \"username=$KC_ADMIN\" -d \"password=$KC_PASS\" \\\n");
        sb.append("  -d \"grant_type=password\" | jq -r '.access_token')\n\n");
        sb.append("if [ -z \"$TOKEN\" ] || [ \"$TOKEN\" = \"null\" ]; then\n");
        sb.append("  echo \"❌ Keycloak 토큰 발급 실패\"\n  exit 1\nfi\n\n");

        // realm
        sb.append("# Realm 생성\n");
        sb.append("echo \"📦 Realm 생성: $REALM\"\n");
        sb.append("curl -sf -X POST \"$KC_URL/admin/realms\" \\\n");
        sb.append("  -H \"Authorization: Bearer $TOKEN\" -H \"Content-Type: application/json\" \\\n");
        sb.append("  -d '{\"realm\":\"'$REALM'\",\"enabled\":true,\"sslRequired\":\"external\"}' || echo \"  (이미 존재)\"\n\n");

        sb.append("echo \"\"\necho \"========================================\"\n");
        sb.append("echo \"  Phase 1: OIDC 클라이언트 등록 + Secret 발급\"\n");
        sb.append("echo \"========================================\"\n\n");

        // 클라이언트 등록 + secret -> sed 자동 치환
        List<String> ssoAddonNames = new java.util.ArrayList<>();
        for (Map<String, Object> a : addons) {
            if (Boolean.TRUE.equals(a.get("keycloakEnabled")) && !"keycloak".equals(a.get("name"))) {
                String n = (String) a.get("name");
                String displayName = (String) a.get("displayName");
                String varName = "SECRET_" + n.toUpperCase().replace("-", "_");
                ssoAddonNames.add(n);

                sb.append("# -- ").append(displayName).append(" --\n");
                sb.append("echo \"🔧 ").append(displayName).append(" 클라이언트 등록\"\n");
                sb.append("curl -sf -X POST \"$KC_URL/admin/realms/$REALM/clients\" \\\n");
                sb.append("  -H \"Authorization: Bearer $TOKEN\" -H \"Content-Type: application/json\" \\\n");
                sb.append("  -d '{\n");
                sb.append("    \"clientId\": \"").append(n).append("\",\n");
                sb.append("    \"name\": \"").append(displayName).append("\",\n");
                sb.append("    \"enabled\": true,\n");
                sb.append("    \"publicClient\": false,\n");
                sb.append("    \"clientAuthenticatorType\": \"client-secret\",\n");
                sb.append("    \"standardFlowEnabled\": true,\n");
                sb.append("    \"directAccessGrantsEnabled\": false,\n");
                sb.append("    \"protocol\": \"openid-connect\",\n");
                sb.append("    \"redirectUris\": [\"https://").append(n).append(".").append(domain).append("/*\"],\n");
                sb.append("    \"webOrigins\": [\"https://").append(n).append(".").append(domain).append("\"]\n");
                sb.append("  }' 2>/dev/null || echo \"  (이미 존재)\"\n\n");

                // secret 조회
                sb.append("CLIENT_UUID=$(curl -sf \"$KC_URL/admin/realms/$REALM/clients?clientId=").append(n).append("\" \\\n");
                sb.append("  -H \"Authorization: Bearer $TOKEN\" | jq -r '.[0].id')\n");
                sb.append(varName).append("=$(curl -sf \"$KC_URL/admin/realms/$REALM/clients/$CLIENT_UUID/client-secret\" \\\n");
                sb.append("  -H \"Authorization: Bearer $TOKEN\" | jq -r '.value')\n");
                sb.append("echo \"  ✅ ").append(n).append(": clientSecret=$").append(varName).append("\"\n\n");

                // sed로 values 파일 자동 치환
                sb.append("# values/").append(n).append("-keycloak.yaml 에 Secret 자동 반영\n");
                sb.append("if [ -f \"$BASE_DIR/values/").append(n).append("-keycloak.yaml\" ]; then\n");
                sb.append("  replace_in_file \"$BASE_DIR/values/").append(n).append("-keycloak.yaml\" \"$PLACEHOLDER\" \"$").append(varName).append("\"\n");
                sb.append("  echo \"  📝 values/").append(n).append("-keycloak.yaml 업데이트 완료\"\n");
                sb.append("fi\n\n");
            }
        }

        // Phase 2: SSO 대상 재배포
        sb.append("echo \"\"\necho \"========================================\"\n");
        sb.append("echo \"  Phase 2: SSO 대상 애드온 재배포 (Secret 반영)\"\n");
        sb.append("echo \"========================================\"\n\n");

        for (Map<String, Object> a : addons) {
            if (Boolean.TRUE.equals(a.get("keycloakEnabled")) && !"keycloak".equals(a.get("name"))) {
                String n = (String) a.get("name");
                String displayName = (String) a.get("displayName");
                String helmChartName = (String) a.get("helmChartName");
                String chartRef = (helmChartName != null && !helmChartName.isEmpty())
                        ? n + "/" + helmChartName : n + "/" + n;

                sb.append("echo \"🔄 ").append(displayName).append(" 재배포\"\n");
                sb.append("helm upgrade --install ").append(n).append(" ").append(chartRef);
                sb.append(" -n $NAMESPACE");
                sb.append(" -f \"$BASE_DIR/values/").append(n).append(".yaml\"");
                if (request.isTlsEnabled()) {
                    sb.append(" -f \"$BASE_DIR/values/").append(n).append("-tls.yaml\"");
                }
                sb.append(" -f \"$BASE_DIR/values/").append(n).append("-keycloak.yaml\"");
                sb.append(" --wait --timeout 600s\n");
                sb.append("echo \"  ✅ ").append(displayName).append(" 재배포 완료\"\n\n");
            }
        }

        sb.append("echo \"\"\necho \"========================================\"\n");
        sb.append("echo \"  ✅ Keycloak SSO 설정 완료\"\n");
        sb.append("echo \"  - Realm: $REALM\"\n");
        sb.append("echo \"  - 클라이언트: ").append(String.join(", ", ssoAddonNames)).append("\"\n");
        sb.append("echo \"  - 모든 Secret이 values 파일에 자동 반영됨\"\n");
        sb.append("echo \"========================================\"\n");
        writeFile(scriptsDir.resolve("configure-keycloak.sh"), sb.toString());
    }

    private void generateInstallScript(Path buildDir, List<Map<String, Object>> addons,
                                       PackageBuildDto.BuildRequest request) throws IOException {
        Path scriptsDir = buildDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("# deploy.sh의 간편 래퍼\n");
        sb.append("set -e\n");
        sb.append("cd \"$(dirname \"$0\")/..\"\n\n");
        sb.append("bash deploy.sh install-all\n");
        writeFile(scriptsDir.resolve("install.sh"), sb.toString());
    }

    private void generateReadme(Path buildDir, List<Map<String, Object>> addons,
                                PackageBuildDto.BuildRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# DevOps 애드온 패키지\n\n");
        sb.append("- **배포 모드**: ").append(request.isAirgapped() ? "폐쇄망 (Airgapped)" : "인터넷").append("\n");
        if (request.getRegistryUrl() != null && !request.getRegistryUrl().isBlank()) {
            sb.append("- **레지스트리**: ").append(request.getRegistryUrl()).append("\n");
        }
        sb.append("- **TLS**: ").append(request.isTlsEnabled() ? "활성" : "비활성").append("\n");
        sb.append("- **Keycloak SSO**: ").append(request.isKeycloakEnabled() ? "활성" : "비활성").append("\n\n");

        sb.append("## 애드온 목록\n\n");
        sb.append("| 순서 | 애드온 | 버전 | SSO |\n|------|--------|------|-----|\n");
        addons.stream()
                .sorted(Comparator.comparingInt(a -> (Integer) a.getOrDefault("installOrder", 50)))
                .forEach(a -> sb.append("| ").append(a.get("installOrder")).append(" | ").append(a.get("displayName"))
                        .append(" | ").append(a.get("version")).append(" | ")
                        .append(Boolean.TRUE.equals(a.get("keycloakEnabled")) ? "✅" : "").append(" |\n"));

        sb.append("\n## 사용법\n\n");
        sb.append("```bash\n# 전체 설치\nbash deploy.sh install-all\n\n");
        sb.append("# 개별 설치/삭제\nbash deploy.sh install keycloak\nbash deploy.sh uninstall harbor\n\n");
        sb.append("# 배포 상태 확인\nbash deploy.sh status\n\n");
        sb.append("# 환경 변수 오버라이드\nNAMESPACE=prod DOMAIN=prod.com bash deploy.sh install-all\n```\n");

        if (request.isAirgapped()) {
            sb.append("\n## 폐쇄망 배포 가이드\n\n");
            sb.append("이 패키지에는 Helm Chart(.tgz)와 컨테이너 이미지(.tar)가 포함되어 있습니다.\n\n");
            sb.append("```bash\n");
            sb.append("# 1. 이미지 로드 + 전체 설치 (install-all에서 자동 로드)\n");
            sb.append("bash deploy.sh install-all\n\n");
            sb.append("# 2. (선택) 고객사 내부 레지스트리에 이미지 push\n");
            sb.append("bash scripts/push-to-registry.sh harbor.customer.com\n");
            sb.append("```\n");
        }
        writeFile(buildDir.resolve("README.md"), sb.toString());
    }

    // ======================== 폐쇄망 빌드 (helm pull + docker pull/save) ========================

    /**
     * 각 애드온의 helm chart를 다운로드하여 charts/ 디렉토리에 저장
     */
    private void pullHelmCharts(Path buildDir, List<Map<String, Object>> addons,
                                PackageBuild build) throws IOException, InterruptedException {
        Path chartsDir = buildDir.resolve("charts");
        Files.createDirectories(chartsDir);

        for (Map<String, Object> a : addons) {
            String name = (String) a.get("name");
            String helmRepo = (String) a.get("helmRepoUrl");
            String helmChartName = (String) a.get("helmChartName");
            String helmChartVersion = (String) a.get("helmChartVersion");

            if (helmRepo == null || helmRepo.isEmpty()) continue;

            String chartFullName = (helmChartName != null && !helmChartName.isEmpty())
                    ? helmChartName : name;

            // 1. helm repo add
            exec("helm", "repo", "add", name, helmRepo);

            // 2. helm repo update
            exec("helm", "repo", "update", name);

            // 3. helm pull → charts/{name}-{version}.tgz
            List<String> pullCmd = new ArrayList<>(List.of(
                    "helm", "pull", name + "/" + chartFullName,
                    "-d", chartsDir.toString(), "--untar=false"
            ));
            if (helmChartVersion != null && !helmChartVersion.isEmpty()) {
                pullCmd.addAll(List.of("--version", helmChartVersion));
            }
            int code = exec(pullCmd.toArray(new String[0]));
            if (code == 0) {
                log.info("  ✅ helm chart pull: {}/{}", name, chartFullName);
            } else {
                log.warn("  ⚠️ helm chart pull 실패: {}/{}", name, chartFullName);
            }
        }
    }

    /**
     * 각 애드온의 컨테이너 이미지를 pull → save (tar) → images/ 디렉토리에 저장
     * registryUrl이 있으면 해당 레지스트리에서 pull, 없으면 upstream에서 pull
     */
    private void pullAndSaveImages(Path buildDir, List<Map<String, Object>> addons,
                                   PackageBuildDto.BuildRequest request,
                                   PackageBuild build) throws IOException, InterruptedException {
        Path imagesDir = buildDir.resolve("images");
        Files.createDirectories(imagesDir);

        String registryUrl = request.getRegistryUrl();
        boolean useRegistry = (registryUrl != null && !registryUrl.isBlank());

        List<String> allImages = resolveImageList(addons);
        int total = allImages.size();
        int done = 0;

        for (String image : allImages) {
            String pullTarget = image;
            if (useRegistry) {
                // harbor.company.com/library/keycloak/keycloak:26.0.7
                String imagePath = image.contains("/") ? image : "library/" + image;
                pullTarget = registryUrl.replaceAll("/$", "") + "/" + imagePath;
            }

            // docker pull
            log.info("  docker pull: {}", pullTarget);
            int pullCode = exec("docker", "pull", pullTarget);
            if (pullCode != 0) {
                log.warn("  ⚠️ docker pull 실패: {} (스킵)", pullTarget);
                done++;
                continue;
            }

            // docker save → images/{safe-filename}.tar
            String safeFileName = image.replaceAll("[/:@]", "_") + ".tar";
            Path tarPath = imagesDir.resolve(safeFileName);
            int saveCode = exec("docker", "save", "-o", tarPath.toString(), pullTarget);
            if (saveCode == 0) {
                log.info("  ✅ docker save: {} → {}", pullTarget, safeFileName);
            } else {
                log.warn("  ⚠️ docker save 실패: {}", pullTarget);
            }

            done++;
            // 이미지 진행률: 55~80% 구간에서 분배
            int imgProgress = 55 + (int) ((done / (double) total) * 25);
            updateProgress(build, imgProgress);
        }
    }

    /**
     * addons의 upstreamImages를 파싱하여 image:tag 목록 반환
     */
    private List<String> resolveImageList(List<Map<String, Object>> addons) {
        List<String> result = new ArrayList<>();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (Map<String, Object> a : addons) {
            String images = (String) a.get("upstreamImages");
            String version = (String) a.get("version");
            if (images == null || images.isEmpty()) continue;
            try {
                List<String> list = mapper.readValue(images,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
                for (String img : list) {
                    String tag = (!img.contains("/")) ? "latest" : (version != null ? version : "latest");
                    result.add(img + ":" + tag);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * 고객사 내부 레지스트리에 이미지 push 스크립트 생성
     */
    private void generatePushToRegistryScript(Path buildDir, List<Map<String, Object>> addons,
                                              PackageBuildDto.BuildRequest request) throws IOException {
        Path scriptsDir = buildDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");
        sb.append("SCRIPT_DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n");
        sb.append("BASE_DIR=\"$(dirname \"$SCRIPT_DIR\")\"\n\n");
        sb.append("# ============================================================\n");
        sb.append("# 고객사 내부 레지스트리에 이미지 push\n");
        sb.append("# 사용법: bash push-to-registry.sh <registry-url>\n");
        sb.append("# 예시:   bash push-to-registry.sh harbor.customer.com\n");
        sb.append("# ============================================================\n\n");

        sb.append("REGISTRY=\"${1:-${REGISTRY:-}}\"\n");
        sb.append("if [ -z \"$REGISTRY\" ]; then\n");
        sb.append("  echo \"사용법: $0 <registry-url>\"\n");
        sb.append("  echo \"예시:   $0 harbor.customer.com\"\n");
        sb.append("  exit 1\n");
        sb.append("fi\n\n");

        sb.append("echo \"========================================\"\n");
        sb.append("echo \"  이미지 로드 + 태그 + Push\"\n");
        sb.append("echo \"  대상 레지스트리: $REGISTRY\"\n");
        sb.append("echo \"========================================\"\n\n");

        // 이미지 로드 → 태그 → push
        List<String> allImages = resolveImageList(addons);
        sb.append("echo \"📦 이미지 로드 중...\"\n");
        sb.append("for img in $BASE_DIR/images/*.tar; do\n");
        sb.append("  [ -f \"$img\" ] || continue\n");
        sb.append("  echo \"  로드: $(basename $img)\"\n");
        sb.append("  docker load -i \"$img\"\n");
        sb.append("done\n\n");

        sb.append("echo \"\"\necho \"🏷️  태그 + Push 시작...\"\n\n");

        for (String image : allImages) {
            String imagePath = image.contains("/") ? image : "library/" + image;
            sb.append("echo \"  push: ").append(image).append("\"\n");
            sb.append("docker tag ").append(image).append(" \"$REGISTRY/").append(imagePath).append("\" 2>/dev/null || true\n");
            sb.append("docker push \"$REGISTRY/").append(imagePath).append("\" 2>/dev/null || echo \"    ⚠️ push 실패: ").append(image).append("\"\n\n");
        }

        sb.append("echo \"\"\n");
        sb.append("echo \"========================================\"\n");
        sb.append("echo \"  ✅ Push 완료: $REGISTRY\"\n");
        sb.append("echo \"========================================\"\n");
        writeFile(scriptsDir.resolve("push-to-registry.sh"), sb.toString());
    }

    /**
     * 외부 프로세스 실행 헬퍼
     */
    private int exec(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command)
                .redirectErrorStream(true);
        Process process = pb.start();

        // 출력 소비 (프로세스 블로킹 방지)
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[exec] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("[exec] 종료 코드 {}: {}", exitCode, String.join(" ", command));
        }
        return exitCode;
    }

    // ======================== 유틸리티 ========================

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String sanitizeDomain(String domain) {
        if (domain == null || domain.isBlank()) return "example.com";
        return domain.replaceAll("[,;\\s]+", "").trim();
    }

    private String sanitizeNamespace(String ns) {
        if (ns == null || ns.isBlank()) return "devops";
        return ns.replaceAll("[^a-zA-Z0-9\\-]", "").trim();
    }

    private void updateProgress(PackageBuild build, int progress) {
        build.setProgress(progress);
        buildRepository.save(build);
    }

    private long countFiles(Path dir) {
        try { return Files.walk(dir).filter(Files::isRegularFile).count(); }
        catch (Exception e) { return -1; }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }
}
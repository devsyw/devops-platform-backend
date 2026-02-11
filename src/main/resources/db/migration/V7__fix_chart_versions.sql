-- ============================================================
-- V7: Chart 버전 수정 및 deprecated 이미지 정리
-- ============================================================

-- 1. Nexus: helm chart 버전 72.0.0 → 64.2.0
--    sonatype/helm3-charts repo가 64.2.0 이후 deprecated
--    chart version과 app version은 다름 (64.2.0 chart → nexus 3.64.0)
--    nexus 3.72.0 이미지는 values.yaml image.tag로 오버라이드
UPDATE addon_version SET helm_chart_version = '64.2.0'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'nexus') AND is_latest = TRUE;

-- 2. Keycloak: keycloakx chart 2.4.0 → 2.4.2 (최신 bugfix)
--    codecentric/helm-charts repo는 여전히 활성
UPDATE addon_version SET helm_chart_version = '2.4.2'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'keycloak') AND is_latest = TRUE;

-- 3. Monitoring: upstream_images에서 jimmidyson/configmap-reload 제거
--    kube-prometheus-stack 65.0.0에서 prometheus-config-reloader로 대체됨
UPDATE addon SET upstream_images =
                     '["grafana/grafana","grafana/loki","prom/prometheus","prom/alertmanager"]'
WHERE name = 'monitoring';

-- 4. Monitoring: imageTags에서 configmap-reload 제거
UPDATE addon_version SET image_tags =
                             '{"grafana":"11.3.0","loki":"3.2.0","prometheus":"v2.54.0","alertmanager":"v0.27.0"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'monitoring') AND is_latest = TRUE;
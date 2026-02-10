-- ============================================================
-- V5: 이미지 태그 완성 및 upstream_images 수정
-- 문제: imageTags에 일부 이미지만 매핑 → 누락된 이미지가 addon version을 태그로 사용하여 pull 실패
-- 예: goharbor/harbor-jobservice:2.11.0 (❌) → v2.11.0 (✅)
-- ============================================================

-- 1. Harbor: upstream_images 수정 (harbor-registry → registry-photon, 실제 Docker Hub 이미지명)
UPDATE addon SET upstream_images =
                     '["goharbor/harbor-core","goharbor/harbor-portal","goharbor/harbor-jobservice","goharbor/registry-photon","goharbor/harbor-registryctl","goharbor/harbor-db","goharbor/redis-photon","goharbor/trivy-adapter-photon"]'
WHERE name = 'harbor';

-- 2. Cert-Manager: imageTags 완성 (startupapicheck 추가)
UPDATE addon_version SET image_tags =
                             '{"cert-manager-controller":"v1.14.5","cert-manager-webhook":"v1.14.5","cert-manager-cainjector":"v1.14.5","cert-manager-startupapicheck":"v1.14.5"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'cert-manager') AND is_latest = TRUE;

-- 3. Harbor: imageTags 전체 8개 이미지 매핑 (모두 v 접두사)
UPDATE addon_version SET image_tags =
                             '{"harbor-core":"v2.11.0","harbor-portal":"v2.11.0","harbor-jobservice":"v2.11.0","registry-photon":"v2.11.0","harbor-registryctl":"v2.11.0","harbor-db":"v2.11.0","redis-photon":"v2.11.0","trivy-adapter-photon":"v2.11.0"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'harbor') AND is_latest = TRUE;

-- 4. Monitoring: imageTags 전체 5개 이미지 매핑 (alertmanager, configmap-reload 추가)
UPDATE addon_version SET image_tags =
                             '{"grafana":"11.3.0","loki":"3.2.0","prometheus":"v2.54.0","alertmanager":"v0.27.0","configmap-reload":"v0.13.1"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'monitoring') AND is_latest = TRUE;

-- 5. Service Mesh (Istio): imageTags 수정
UPDATE addon_version SET image_tags =
                             '{"pilot":"1.23.0","proxyv2":"1.23.0"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'service-mesh') AND is_latest = TRUE;
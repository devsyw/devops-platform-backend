-- ============================================================
-- V8: ArgoCD redis 태그 고정 + Monitoring 이미지 prefix 수정
-- ============================================================

-- 1. ArgoCD: imageTags에 redis 태그 추가 (latest → 7.4.1-alpine)
UPDATE addon_version SET image_tags =
                             '{"argocd":"v2.13.2","redis":"7.4.1-alpine"}'
WHERE addon_id = (SELECT id FROM addon WHERE name = 'argocd') AND is_latest = TRUE;

-- 2. Monitoring: upstream_images의 prom/ prefix → 실제 Docker Hub 경로
--    Docker Hub에서 prom/prometheus, prom/alertmanager는 존재하지 않음
--    실제 경로: prometheus → prom/prometheus (OK), alertmanager → prom/alertmanager (OK)
--    확인 결과 prom/ prefix는 정상이므로 변경 불필요
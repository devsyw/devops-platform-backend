-- ============================================================
-- V6: 패키지 빌드에 CPU 아키텍처(platform) 선택 기능 추가
-- 값: linux/amd64, linux/arm64, linux/amd64,linux/arm64 (멀티)
-- ============================================================

ALTER TABLE package_build ADD COLUMN platform VARCHAR(100) DEFAULT 'linux/amd64';
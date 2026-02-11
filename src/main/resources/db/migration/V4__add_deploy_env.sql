-- 배포 환경 타입 (INTERNET / AIRGAPPED)
ALTER TABLE package_build ADD COLUMN deploy_env VARCHAR(50) NOT NULL DEFAULT 'INTERNET';

-- 사용자 지정 레지스트리 URL (예: harbor.company.com)
ALTER TABLE package_build ADD COLUMN registry_url VARCHAR(500);

-- ============================================================
-- V1: DevOps Platform 초기 스키마
-- ============================================================

-- 고객사
CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) UNIQUE,
    environment VARCHAR(50),
    k8s_version VARCHAR(50),
    os_info VARCHAR(200),
    node_count INT,
    storage_info TEXT,
    network_info TEXT,
    vpn_info TEXT,
    contact_name VARCHAR(100),
    contact_email VARCHAR(200),
    contact_phone VARCHAR(50),
    memo TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 애드온
CREATE TABLE addon (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    upstream_images TEXT,
    helm_repo_url VARCHAR(500),
    helm_chart_name VARCHAR(200),
    keycloak_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    keycloak_client_template TEXT,
    keycloak_values_template TEXT,
    install_order INT NOT NULL DEFAULT 50,
    dependencies TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 애드온 버전
CREATE TABLE addon_version (
    id BIGSERIAL PRIMARY KEY,
    addon_id BIGINT NOT NULL,
    version VARCHAR(100) NOT NULL,
    image_tags TEXT,
    helm_chart_version VARCHAR(100),
    is_latest BOOLEAN NOT NULL DEFAULT FALSE,
    release_note_url VARCHAR(500),
    synced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (addon_id) REFERENCES addon(id)
);

-- 설치 이력
CREATE TABLE installation (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    installed_at TIMESTAMP,
    installed_by VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    package_hash VARCHAR(100),
    package_url VARCHAR(500),
    keycloak_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tls_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    namespace VARCHAR(200),
    domain VARCHAR(500),
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- 설치-애드온 매핑
CREATE TABLE install_addon (
    id BIGSERIAL PRIMARY KEY,
    installation_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    version VARCHAR(100) NOT NULL,
    helm_chart_version VARCHAR(100),
    FOREIGN KEY (installation_id) REFERENCES installation(id),
    FOREIGN KEY (addon_id) REFERENCES addon(id)
);

-- 인증서
CREATE TABLE certificate (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    domain VARCHAR(500) NOT NULL,
    issued_at DATE,
    expires_at DATE NOT NULL,
    issuer VARCHAR(200),
    cert_type VARCHAR(50),
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- 인증서 갱신 이력
CREATE TABLE cert_renewal_history (
    id BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    prev_expires_at DATE NOT NULL,
    new_expires_at DATE NOT NULL,
    renewed_at TIMESTAMP NOT NULL,
    renewed_by VARCHAR(100),
    memo TEXT,
    FOREIGN KEY (certificate_id) REFERENCES certificate(id)
);

-- 패키지 빌드
CREATE TABLE package_build (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT,
    build_hash VARCHAR(100) NOT NULL UNIQUE,
    selected_addons TEXT NOT NULL,
    total_size BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'BUILDING',
    file_path VARCHAR(500),
    built_by VARCHAR(100),
    keycloak_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    tls_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    namespace VARCHAR(200),
    domain VARCHAR(500),
    progress INT DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- Harbor 동기화 로그
CREATE TABLE harbor_sync_log (
    id BIGSERIAL PRIMARY KEY,
    addon_id BIGINT NOT NULL,
    sync_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    new_versions_found TEXT,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    FOREIGN KEY (addon_id) REFERENCES addon(id)
);

-- 알림
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    message TEXT,
    addon_id BIGINT,
    customer_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (addon_id) REFERENCES addon(id),
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- 인덱스
CREATE INDEX idx_addon_version_addon_id ON addon_version(addon_id);
CREATE INDEX idx_addon_version_is_latest ON addon_version(is_latest);
CREATE INDEX idx_installation_customer_id ON installation(customer_id);
CREATE INDEX idx_certificate_customer_id ON certificate(customer_id);
CREATE INDEX idx_certificate_expires_at ON certificate(expires_at);
CREATE INDEX idx_package_build_hash ON package_build(build_hash);
CREATE INDEX idx_notification_is_read ON notification(is_read);
CREATE INDEX idx_notification_created_at ON notification(created_at);

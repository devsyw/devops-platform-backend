-- ============================================================
-- V3: 프로젝트 테이블 추가 (고객사 > 프로젝트 트리구조)
-- ============================================================

CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100),
    description TEXT,
    k8s_version VARCHAR(50),
    namespace VARCHAR(200),
    domain VARCHAR(500),
    environment VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE INDEX idx_project_customer_id ON project(customer_id);

-- installation 테이블에 project_id 컬럼 추가
ALTER TABLE installation ADD COLUMN project_id BIGINT;
ALTER TABLE installation ADD FOREIGN KEY (project_id) REFERENCES project(id);

-- package_build 테이블에 project_id 컬럼 추가
ALTER TABLE package_build ADD COLUMN project_id BIGINT;
ALTER TABLE package_build ADD FOREIGN KEY (project_id) REFERENCES project(id);

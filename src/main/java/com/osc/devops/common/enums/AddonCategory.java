package com.osc.devops.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddonCategory {

    SECURITY("보안/인증"),
    CI_CD("CI/CD"),
    MONITORING("모니터링"),
    SOURCE("소스관리"),
    INFRA("인프라"),
    ARTIFACT("아티팩트"),
    NETWORK("네트워크"),
    QUALITY("품질관리");

    private final String displayName;
}

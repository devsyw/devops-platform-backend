package com.osc.devops.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerCreateRequest {
    @NotBlank(message = "고객사명은 필수입니다")
    private String name;
    private String code;
    private String environment;
    private String k8sVersion;
    private String osInfo;
    private Integer nodeCount;
    private String storageInfo;
    private String networkInfo;
    private String vpnInfo;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String memo;
}

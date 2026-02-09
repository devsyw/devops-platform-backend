package com.osc.devops.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerUpdateRequest {
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

package com.osc.devops.customer.dto;

import com.osc.devops.customer.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder @AllArgsConstructor
public class CustomerResponse {
    private Long id;
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
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer entity) {
        return CustomerResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .environment(entity.getEnvironment())
                .k8sVersion(entity.getK8sVersion())
                .osInfo(entity.getOsInfo())
                .nodeCount(entity.getNodeCount())
                .storageInfo(entity.getStorageInfo())
                .networkInfo(entity.getNetworkInfo())
                .vpnInfo(entity.getVpnInfo())
                .contactName(entity.getContactName())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .memo(entity.getMemo())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

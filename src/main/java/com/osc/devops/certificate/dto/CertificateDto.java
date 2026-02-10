package com.osc.devops.certificate.dto;

import com.osc.devops.certificate.entity.Certificate;
import com.osc.devops.common.enums.CertStatus;
import com.osc.devops.common.enums.CertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CertificateDto {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotNull private Long customerId;
        @NotBlank private String domain;
        private LocalDate issuedAt;
        @NotNull private LocalDate expiresAt;
        private String issuer;
        private CertType certType;
        private Boolean autoRenew;
        private String memo;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        private String domain;
        private LocalDate issuedAt;
        private LocalDate expiresAt;
        private String issuer;
        private CertType certType;
        private Boolean autoRenew;
        private CertStatus status;
        private String memo;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RenewRequest {
        @NotNull private LocalDate newExpiresAt;
        private String renewedBy;
        private String memo;
    }

    @Getter @Builder @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long customerId;
        private String customerName;
        private String domain;
        private LocalDate issuedAt;
        private LocalDate expiresAt;
        private String issuer;
        private CertType certType;
        private Boolean autoRenew;
        private CertStatus status;
        private String memo;
        private long daysUntilExpiry;
        private LocalDateTime createdAt;

        public static Response from(Certificate e) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e.getExpiresAt());
            return Response.builder()
                    .id(e.getId()).customerId(e.getCustomer().getId()).customerName(e.getCustomer().getName())
                    .domain(e.getDomain()).issuedAt(e.getIssuedAt()).expiresAt(e.getExpiresAt())
                    .issuer(e.getIssuer()).certType(e.getCertType()).autoRenew(e.getAutoRenew())
                    .status(e.getStatus()).memo(e.getMemo()).daysUntilExpiry(days).createdAt(e.getCreatedAt())
                    .build();
        }
    }
}

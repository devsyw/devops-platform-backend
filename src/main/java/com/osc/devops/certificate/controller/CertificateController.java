package com.osc.devops.certificate.controller;

import com.osc.devops.certificate.dto.CertificateDto;
import com.osc.devops.certificate.service.CertificateService;
import com.osc.devops.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certService;

    @GetMapping
    public ApiResponse<Page<CertificateDto.Response>> list(
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(certService.getCertificates(customerId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CertificateDto.Response> get(@PathVariable Long id) {
        return ApiResponse.ok(certService.getCertificate(id));
    }

    @GetMapping("/expiring")
    public ApiResponse<List<CertificateDto.Response>> expiring(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(certService.getExpiringSoon(days));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CertificateDto.Response> create(@Valid @RequestBody CertificateDto.CreateRequest req) {
        return ApiResponse.ok(certService.create(req), "인증서가 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<CertificateDto.Response> update(@PathVariable Long id, @RequestBody CertificateDto.UpdateRequest req) {
        return ApiResponse.ok(certService.update(id, req));
    }

    @PostMapping("/{id}/renew")
    public ApiResponse<CertificateDto.Response> renew(@PathVariable Long id, @Valid @RequestBody CertificateDto.RenewRequest req) {
        return ApiResponse.ok(certService.renew(id, req), "인증서가 갱신되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        certService.delete(id);
        return ApiResponse.ok("인증서가 삭제되었습니다.");
    }
}

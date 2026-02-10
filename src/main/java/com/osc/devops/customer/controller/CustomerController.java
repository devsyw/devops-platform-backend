package com.osc.devops.customer.controller;

import com.osc.devops.common.dto.ApiResponse;
import com.osc.devops.common.dto.PageResponse;
import com.osc.devops.customer.dto.CustomerCreateRequest;
import com.osc.devops.customer.dto.CustomerResponse;
import com.osc.devops.customer.dto.CustomerUpdateRequest;
import com.osc.devops.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeInactive,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(customerService.getCustomers(keyword, includeInactive, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getCustomer(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        return ApiResponse.ok(customerService.createCustomer(request), "고객사가 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> updateCustomer(@PathVariable Long id, @RequestBody CustomerUpdateRequest request) {
        return ApiResponse.ok(customerService.updateCustomer(id, request), "고객사 정보가 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResponse.ok("고객사가 비활성화되었습니다.");
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<CustomerResponse> activateCustomer(@PathVariable Long id) {
        return ApiResponse.ok(customerService.activateCustomer(id), "고객사가 활성화되었습니다.");
    }
}
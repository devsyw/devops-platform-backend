package com.osc.devops.customer.service;

import com.osc.devops.common.exception.NotFoundException;
import com.osc.devops.customer.dto.CustomerCreateRequest;
import com.osc.devops.customer.dto.CustomerResponse;
import com.osc.devops.customer.dto.CustomerUpdateRequest;
import com.osc.devops.customer.entity.Customer;
import com.osc.devops.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Page<CustomerResponse> getCustomers(String keyword, Pageable pageable) {
        Page<Customer> page = (keyword != null && !keyword.isBlank())
                ? customerRepository.searchByKeyword(keyword, pageable)
                : customerRepository.findByIsActiveTrue(pageable);
        return page.map(CustomerResponse::from);
    }

    public CustomerResponse getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다. id=" + id));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = Customer.builder()
                .name(request.getName())
                .code(request.getCode())
                .environment(request.getEnvironment())
                .k8sVersion(request.getK8sVersion())
                .osInfo(request.getOsInfo())
                .nodeCount(request.getNodeCount())
                .storageInfo(request.getStorageInfo())
                .networkInfo(request.getNetworkInfo())
                .vpnInfo(request.getVpnInfo())
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .memo(request.getMemo())
                .build();
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다. id=" + id));
        if (request.getName() != null) customer.setName(request.getName());
        if (request.getCode() != null) customer.setCode(request.getCode());
        if (request.getEnvironment() != null) customer.setEnvironment(request.getEnvironment());
        if (request.getK8sVersion() != null) customer.setK8sVersion(request.getK8sVersion());
        if (request.getOsInfo() != null) customer.setOsInfo(request.getOsInfo());
        if (request.getNodeCount() != null) customer.setNodeCount(request.getNodeCount());
        if (request.getStorageInfo() != null) customer.setStorageInfo(request.getStorageInfo());
        if (request.getNetworkInfo() != null) customer.setNetworkInfo(request.getNetworkInfo());
        if (request.getVpnInfo() != null) customer.setVpnInfo(request.getVpnInfo());
        if (request.getContactName() != null) customer.setContactName(request.getContactName());
        if (request.getContactEmail() != null) customer.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) customer.setContactPhone(request.getContactPhone());
        if (request.getMemo() != null) customer.setMemo(request.getMemo());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("고객사를 찾을 수 없습니다. id=" + id));
        customer.setIsActive(false);
        customerRepository.save(customer);
    }
}

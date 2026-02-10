package com.osc.devops.customer.repository;

import com.osc.devops.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findByIsActiveTrue(Pageable pageable);
    Page<Customer> findAllByOrderByIsActiveDescCreatedAtDesc(Pageable pageable);
    @Query("SELECT c FROM Customer c WHERE c.isActive = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    @Query("SELECT c FROM Customer c WHERE (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY c.isActive DESC, c.createdAt DESC")
    Page<Customer> searchByKeywordIncludeInactive(@Param("keyword") String keyword, Pageable pageable);
    long countByIsActiveTrue();
    java.util.Optional<Customer> findByCode(String code);
}
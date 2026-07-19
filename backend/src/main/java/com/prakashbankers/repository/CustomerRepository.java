package com.prakashbankers.repository;

import com.prakashbankers.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.loans WHERE c.id = :id")
    Optional<Customer> findByIdWithLoans(Long id);

    @Query("SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.loans")
    List<Customer> findAllWithLoans();
}
package com.prakashbankers.repository;

import com.prakashbankers.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanId(String loanId);

    @Query("SELECT l FROM Loan l JOIN FETCH l.customer LEFT JOIN FETCH l.repledge r LEFT JOIN FETCH r.place LEFT JOIN FETCH r.repledger WHERE l.loanId = :loanId")
    Optional<Loan> findByLoanIdWithCustomerAndRepledge(String loanId);

    @Query("SELECT DISTINCT l FROM Loan l LEFT JOIN FETCH l.transactions WHERE l.loanId = :loanId")
    Optional<Loan> findByLoanIdWithLoanTransactions(String loanId);

    @Query("SELECT DISTINCT r FROM Repledge r LEFT JOIN FETCH r.transactions WHERE r.loan.loanId = :loanId")
    Optional<com.prakashbankers.entity.Repledge> findRepledgeWithTransactions(String loanId);

    @Query("SELECT DISTINCT l FROM Loan l LEFT JOIN FETCH l.transactions WHERE l IN :loans")
    List<Loan> fetchTransactionsForLoans(@Param("loans") List<Loan> loans);

    @Query("SELECT DISTINCT l FROM Loan l JOIN FETCH l.customer LEFT JOIN FETCH l.transactions")
    List<Loan> findAllWithDetails();

    @Query("SELECT DISTINCT l FROM Loan l JOIN FETCH l.customer LEFT JOIN FETCH l.repledge r LEFT JOIN FETCH r.place WHERE l.repledge IS NOT NULL")
    List<Loan> findAllRepledgedWithDetails();
}

package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.LoanRepository;
import com.prakashbankers.util.FinancialMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepledgeService {

    private final LoanRepository loanRepository;
    private final InterestEngine interestEngine;
    private final CustomerService customerService;

    public RepledgeService(LoanRepository loanRepository, InterestEngine interestEngine, CustomerService customerService) {
        this.loanRepository = loanRepository;
        this.interestEngine = interestEngine;
        this.customerService = customerService;
    }

    @Transactional(readOnly = true)
    public List<RepledgeListItem> listRepledges(String search) {
        return loanRepository.findAllRepledgedWithDetails().stream()
                .filter(l -> search == null || search.isBlank()
                        || l.getCustomer().getName().toLowerCase().contains(search.toLowerCase())
                        || l.getItem().toLowerCase().contains(search.toLowerCase()))
                .map(l -> {
                    FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(l, LocalDate.now()));
                    return RepledgeListItem.builder()
                            .loanId(l.getLoanId())
                            .customerId(l.getCustomer().getId())
                            .customerName(l.getCustomer().getName())
                            .item(l.getItem())
                            .customerPrincipal(fin.getCurrentPrincipal())
                            .bankBorrowed(l.getRepledge().getAmount())
                            .placeName(l.getRepledge().getPlace().getName())
                            .build();
                }).toList();
    }

    @Transactional(readOnly = true)
    public RepledgeSummary getVaultDetail(String loanId) {
        Loan loan = loanRepository.findByLoanIdWithCustomerAndRepledge(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (loan.getRepledge() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan is not repledged");
        }
        org.hibernate.Hibernate.initialize(loan.getTransactions());
        org.hibernate.Hibernate.initialize(loan.getRepledge().getTransactions());
        LoanResponse lr = customerService.toLoanResponse(loan, LocalDate.now());
        return lr.getRepledge();
    }

    @Transactional
    public RepledgeSummary addBankPayment(String loanId, TransactionType type, PaymentRequest req) {
        Loan loan = loanRepository.findByLoanIdWithCustomerAndRepledge(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        org.hibernate.Hibernate.initialize(loan.getTransactions());
        Repledge rep = loan.getRepledge();
        if (rep == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No repledge on this loan");
        }
        org.hibernate.Hibernate.initialize(rep.getTransactions());

        LocalDate payDate = req.getDate() != null ? req.getDate() : LocalDate.now();
        interestEngine.validatePaymentDate(payDate, rep.getDate());
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.repledgeInput(rep, loan, payDate));
        interestEngine.validatePayment(type, req.getAmount(), fin);

        RepledgeTransaction tx = RepledgeTransaction.builder()
                .repledge(rep)
                .date(payDate)
                .type(type)
                .amount(req.getAmount())
                .build();

        if (type == TransactionType.principal) {
            tx.setRateAtPayment(rep.getRate());
            tx.setInterestDueAtPayment(fin.getInterestBalance());
        }

        if (rep.getTransactions() == null) {
            rep.setTransactions(new ArrayList<>());
        }
        rep.getTransactions().add(tx);
        loanRepository.save(loan);
        return getVaultDetail(loanId);
    }
}

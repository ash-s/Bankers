package com.prakashbankers.util;

import com.prakashbankers.dto.InterestCalcInput;

import com.prakashbankers.dto.LedgerTransactionDto;
import com.prakashbankers.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FinancialMapper {

    private FinancialMapper() {}

    public static List<LedgerTransactionDto> fromLoanTransactions(List<LoanTransaction> txs) {
        return txs.stream().map(t -> LedgerTransactionDto.builder()
                .date(t.getDate())
                .type(t.getType())
                .amount(t.getAmount())
                .build()).toList();
    }

    public static List<LedgerTransactionDto> fromBorrowingTransactions(List<BorrowingTransaction> txs) {
        return txs.stream().map(t -> LedgerTransactionDto.builder()
                .date(t.getDate())
                .type(t.getType())
                .amount(t.getAmount())
                .build()).toList();
    }

    public static List<LedgerTransactionDto> fromRepledgeTransactions(List<RepledgeTransaction> txs) {
        return txs.stream().map(t -> LedgerTransactionDto.builder()
                .date(t.getDate())
                .type(t.getType())
                .amount(t.getAmount())
                .build()).toList();
    }

    public static InterestCalcInput loanInput(Loan loan, LocalDate calcDate) {
        return InterestCalcInput.builder()
                .principal(loan.getPledgeAmount())
                .startDate(loan.getPledgeDate())
                .rate(loan.getInterestRate())
                .interestType(loan.getInterestType())
                .transactions(fromLoanTransactions(loan.getTransactions()))
                .calcDate(calcDate)
                .build();
    }

    public static InterestCalcInput borrowingInput(Borrowing b, LocalDate calcDate) {
        return InterestCalcInput.builder()
                .principal(b.getAmount())
                .startDate(b.getDate())
                .rate(b.getInterestRate())
                .interestType(b.getInterestType())
                .transactions(fromBorrowingTransactions(b.getTransactions()))
                .calcDate(calcDate)
                .build();
    }

    public static InterestCalcInput repledgeInput(Repledge r, Loan loan, LocalDate calcDate) {
        InterestType type = r.getInterestType() != null ? r.getInterestType() : loan.getInterestType();
        return InterestCalcInput.builder()
                .principal(r.getAmount())
                .startDate(r.getDate())
                .rate(r.getRate())
                .interestType(type)
                .transactions(fromRepledgeTransactions(r.getTransactions()))
                .calcDate(calcDate)
                .build();
    }

    public static String temporaryCode(String prefix) {
        return "TMP-" + prefix + "-" + java.util.UUID.randomUUID();
    }

    public static String orderedCode(String prefix, Long id) {
        if (id == null) throw new IllegalArgumentException("Saved entity ID is required");
        return "%s-%06d".formatted(prefix, id);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

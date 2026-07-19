package com.prakashbankers.service;

import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.dto.InterestCalcInput;
import com.prakashbankers.dto.LedgerTransactionDto;
import com.prakashbankers.entity.InterestType;
import com.prakashbankers.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterestEngineTest {

    private final InterestEngine engine = new InterestEngine();

    @Test
    void fullCalendarMonthAtTwoPercentIsExactlyTwoThousand() {
        FinancialSummaryDto result = calculate(
                "100000", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), List.of());

        assertThat(result.getCurrentPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(result.getInterestGenerated()).isEqualByComparingTo("2000.00");
        assertThat(result.getInterestBalance()).isEqualByComparingTo("2000.00");
        assertThat(result.getCycleInterestAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    void calendarMonthsUseTheirActualLength() {
        FinancialSummaryDto february = calculate(
                "100000", LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29), List.of());
        FinancialSummaryDto march = calculate(
                "100000", LocalDate.of(2024, 1, 31), LocalDate.of(2024, 3, 31), List.of());

        assertThat(february.getInterestGenerated()).isEqualByComparingTo("2000.00");
        assertThat(march.getInterestGenerated()).isEqualByComparingTo("4000.00");
    }

    @Test
    void partialPrincipalSplitsInterestInsideCalendarCycle() {
        List<LedgerTransactionDto> transactions = List.of(
                tx(LocalDate.of(2026, 2, 1), TransactionType.principal, "40000"));

        FinancialSummaryDto firstMonth = calculate(
                "100000", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), transactions);
        FinancialSummaryDto secondMonth = calculate(
                "100000", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 15), transactions);

        // Jan 15-Feb 1: 100000 @ 2% for 17/31 days.
        // Feb 1-Feb 15: 60000 @ 2% for 14/31 days.
        assertThat(firstMonth.getCurrentPrincipal()).isEqualByComparingTo("60000.00");
        assertThat(firstMonth.getInterestGenerated()).isEqualByComparingTo("1638.71");
        assertThat(secondMonth.getInterestGenerated()).isEqualByComparingTo("2838.71");
        assertThat(firstMonth.getInterestBreakdown()).hasSize(2);
    }

    @Test
    void paidFirstMonthInterestLeavesPrincipalUntouched() {
        List<LedgerTransactionDto> transactions = List.of(
                tx(LocalDate.of(2026, 2, 15), TransactionType.interest, "2000"));

        FinancialSummaryDto result = calculate(
                "100000", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), transactions);

        assertThat(result.getCurrentPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(result.getInterestGenerated()).isEqualByComparingTo("2000.00");
        assertThat(result.getInterestPaid()).isEqualByComparingTo("2000.00");
        assertThat(result.getInterestBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void futurePrincipalPaymentDoesNotZeroTodaysBalance() {
        List<LedgerTransactionDto> transactions = List.of(
                tx(LocalDate.of(2026, 8, 15), TransactionType.principal, "100000"));

        FinancialSummaryDto result = calculate(
                "100000", LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 18), transactions);

        assertThat(result.getCurrentPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(result.getPrincipalPaid()).isEqualByComparingTo("0.00");
    }

    @Test
    void prepaidInterestShowsAsAdvanceCreditUntilEarned() {
        List<LedgerTransactionDto> transactions = List.of(
                tx(LocalDate.of(2026, 7, 15), TransactionType.interest, "2000"));

        FinancialSummaryDto result = calculate(
                "100000", LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 20), transactions);

        assertThat(result.getInterestBalance()).isEqualByComparingTo("0.00");
        assertThat(result.getAdvanceInterestCredit()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getCurrentPrincipal()).isEqualByComparingTo("100000.00");
    }

    private FinancialSummaryDto calculate(
            String principal,
            LocalDate start,
            LocalDate asOf,
            List<LedgerTransactionDto> transactions) {
        return engine.calculate(InterestCalcInput.builder()
                .principal(new BigDecimal(principal))
                .startDate(start)
                .rate(new BigDecimal("2"))
                .interestType(InterestType.monthly)
                .transactions(transactions)
                .calcDate(asOf)
                .build());
    }

    private LedgerTransactionDto tx(LocalDate date, TransactionType type, String amount) {
        return LedgerTransactionDto.builder()
                .date(date)
                .type(type)
                .amount(new BigDecimal(amount))
                .build();
    }
}

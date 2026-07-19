package com.prakashbankers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDto {
    private BigDecimal currentPrincipal;
    private BigDecimal principalPaid;
    private BigDecimal interestGenerated;
    private BigDecimal interestPaid;
    private BigDecimal discountGiven;
    private BigDecimal interestBalance;
    private BigDecimal advanceInterestCredit;
    private BigDecimal cycleInterestAmount;
    private int overdueCycles;
    private LocalDate asOfDate;
    private List<InterestBreakdownDto> interestBreakdown;
}

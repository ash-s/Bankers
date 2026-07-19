package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.CustomerResponse;
import com.prakashbankers.dto.ApiDtos.LoanResponse;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.InterestType;
import com.prakashbankers.entity.LoanStatus;
import com.prakashbankers.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BillPdfServiceTest {

    private final BillPdfService service = new BillPdfService();

    @Test
    void generatesPdfWithFinancialSummaryFields() {
        CustomerResponse customer = CustomerResponse.builder()
                .name("Test Customer")
                .phone("9999999999")
                .code("CUST-000001")
                .loans(List.of(LoanResponse.builder()
                        .loanId("L-1001")
                        .item("Chain")
                        .material("Gold")
                        .purity("22K")
                        .grossWeight(new BigDecimal("10"))
                        .netWeight(new BigDecimal("10"))
                        .pledgeAmount(new BigDecimal("100000"))
                        .pledgeDate(LocalDate.of(2026, 1, 15))
                        .interestRate(new BigDecimal("2"))
                        .interestType(InterestType.monthly)
                        .status(LoanStatus.active)
                        .financials(FinancialSummaryDto.builder()
                                .currentPrincipal(new BigDecimal("100000"))
                                .principalPaid(BigDecimal.ZERO)
                                .interestGenerated(new BigDecimal("2000"))
                                .interestPaid(BigDecimal.ZERO)
                                .discountGiven(BigDecimal.ZERO)
                                .interestBalance(new BigDecimal("2000"))
                                .advanceInterestCredit(BigDecimal.ZERO)
                                .cycleInterestAmount(new BigDecimal("2000"))
                                .asOfDate(LocalDate.of(2026, 2, 15))
                                .build())
                        .transactions(List.of())
                        .build()))
                .build();

        byte[] pdf = service.generateBill(customer, "L-1001");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(5, pdf.length))).startsWith("%PDF");
    }

    @Test
    void generatesPdfWhenOptionalFinancialFieldsAreNull() {
        CustomerResponse customer = CustomerResponse.builder()
                .name("Test Customer")
                .phone("9999999999")
                .code("CUST-000001")
                .loans(List.of(LoanResponse.builder()
                        .loanId("L-1002")
                        .item("Ring")
                        .material("Gold")
                        .purity("22K")
                        .grossWeight(new BigDecimal("5"))
                        .netWeight(new BigDecimal("5"))
                        .pledgeAmount(new BigDecimal("50000"))
                        .pledgeDate(LocalDate.of(2026, 1, 15))
                        .interestRate(new BigDecimal("2"))
                        .interestType(InterestType.monthly)
                        .status(LoanStatus.active)
                        .financials(FinancialSummaryDto.builder()
                                .currentPrincipal(new BigDecimal("50000"))
                                .interestBalance(new BigDecimal("1000"))
                                .cycleInterestAmount(new BigDecimal("1000"))
                                .build())
                        .transactions(List.of())
                        .build()))
                .build();

        byte[] pdf = service.generateBill(customer, "L-1002");

        assertThat(pdf).isNotEmpty();
    }
}

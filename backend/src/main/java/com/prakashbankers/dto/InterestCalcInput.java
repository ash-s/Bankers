package com.prakashbankers.dto;

import com.prakashbankers.entity.InterestType;
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
public class InterestCalcInput {
    private BigDecimal principal;
    private LocalDate startDate;
    private BigDecimal rate;
    private InterestType interestType;
    private List<LedgerTransactionDto> transactions;
    private LocalDate calcDate;
}

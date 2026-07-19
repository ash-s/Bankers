package com.prakashbankers.dto;

import com.prakashbankers.entity.InterestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestBreakdownDto {
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal principal;
    private BigDecimal rate;
    private InterestType interestType;
    private long days;
    private long cycleDays;
    private BigDecimal interest;
}

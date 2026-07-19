package com.prakashbankers.dto;

import com.prakashbankers.entity.TransactionType;
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
public class LedgerTransactionDto {
    private LocalDate date;
    private TransactionType type;
    private BigDecimal amount;
}

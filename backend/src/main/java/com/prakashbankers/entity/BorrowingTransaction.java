package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "borrowing_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BorrowingTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private Borrowing borrowing;

    @Column(nullable = false)
    private LocalDate date;

    /** principal or interest only — each borrowing has its own separate ledger */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private String note;

    @Column(precision = 8, scale = 4)
    private BigDecimal rateAtPayment;

    @Column(precision = 15, scale = 2)
    private BigDecimal interestDueAtPayment;
}

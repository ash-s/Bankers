package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repledge_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepledgeTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repledge_id", nullable = false)
    private Repledge repledge;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 8, scale = 4)
    private BigDecimal rateAtPayment;

    @Column(precision = 15, scale = 2)
    private BigDecimal interestDueAtPayment;
}

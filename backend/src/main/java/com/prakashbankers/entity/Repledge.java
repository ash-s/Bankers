package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repledges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Repledge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private RepledgePlace place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repledger_id", nullable = false)
    private Repledger repledger;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    private InterestType interestType;

    @OneToMany(mappedBy = "repledge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, id ASC")
    @Builder.Default
    private List<RepledgeTransaction> transactions = new ArrayList<>();
}

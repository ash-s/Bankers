package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private String material;
    private String item;
    private BigDecimal grossWeight;
    private BigDecimal netWeight;
    private String purity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal pledgeAmount;

    @Column(nullable = false)
    private LocalDate pledgeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestType interestType;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private String dismissedNotifDate;

    private String materialPhotoFileName;
    private String materialPhotoFilePath;
    private String materialPhotoMimeType;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, id ASC")
    @Builder.Default
    private List<LoanTransaction> transactions = new ArrayList<>();

    @OneToOne(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Repledge repledge;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = LoanStatus.active;
    }
}

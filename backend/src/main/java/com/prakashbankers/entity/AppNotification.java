package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "app_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String loanId;

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String item;
    private int overdueCycles;
    private BigDecimal interestBalance;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean readFlag = false;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

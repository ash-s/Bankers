package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private String shopName = "Prakash Bankers";

    private String shopAddress;
    private String shopPhone;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal openingCapital = BigDecimal.ZERO;

    private boolean openingCapitalSet;
}

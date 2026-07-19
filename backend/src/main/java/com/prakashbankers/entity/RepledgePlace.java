package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "repledge_places")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepledgePlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceType type;

    private String contact;
    private String address;

    @Column(precision = 8, scale = 4)
    private BigDecimal defaultRate;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (code == null || code.isBlank()) {
            code = "P" + (System.currentTimeMillis() % 1000000);
        }
    }
}

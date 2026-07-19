package com.prakashbankers.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lenders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lender {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String address;

    @Column(precision = 8, scale = 4)
    private BigDecimal defaultRate;

    @OneToMany(mappedBy = "lender", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<Borrowing> borrowings = new ArrayList<>();

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (code == null || code.isBlank()) {
            code = "LND" + (System.currentTimeMillis() % 1000000);
        }
    }
}

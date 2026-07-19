package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "repledgers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Repledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String role;
    private String phone;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (code == null || code.isBlank()) {
            code = "R" + (System.currentTimeMillis() % 1000000);
        }
    }
}

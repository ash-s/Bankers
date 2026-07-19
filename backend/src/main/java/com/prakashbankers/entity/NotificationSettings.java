package com.prakashbankers.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Preset: 30d, 180d, 365d, or custom */
    @Column(nullable = false)
    @Builder.Default
    private String duePreset = "30d";

    /** Manual days when preset is custom */
    @Builder.Default
    private Integer customDueDays = 30;

    @Builder.Default
    private boolean pushEnabled = true;

    @Builder.Default
    private boolean smsEnabled = false;

    @Builder.Default
    private boolean whatsappEnabled = false;

    private String smsTemplate;

    private String whatsappTemplate;
}

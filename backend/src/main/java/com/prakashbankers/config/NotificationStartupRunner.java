package com.prakashbankers.config;

import com.prakashbankers.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationStartupRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationStartupRunner.class);
    private final NotificationService notificationService;

    public NotificationStartupRunner(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            notificationService.syncOnStartup();
        } catch (Exception e) {
            log.warn("Notification sync on startup skipped: {}", e.getMessage());
        }
    }
}

package com.prakashbankers.repository;

import com.prakashbankers.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findAllByOrderByCreatedAtDesc();
    List<AppNotification> findByReadFlagFalseOrderByCreatedAtDesc();
    Optional<AppNotification> findByLoanId(String loanId);
    long countByReadFlagFalse();
}

package com.prakashbankers.config;

import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           MaterialRepository materialRepository, AppSettingsRepository appSettingsRepository,
                           AppNotificationRepository notificationRepository, LoanRepository loanRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .displayName("Shop Owner")
                        .role(UserRole.OWNER)
                        .build());
            }

            if (materialRepository.count() == 0) {
                materialRepository.save(Material.builder().name("Gold").build());
                materialRepository.save(Material.builder().name("Silver").build());
                materialRepository.save(Material.builder().name("Platinum").build());
            }

            if (appSettingsRepository.count() == 0) {
                appSettingsRepository.save(AppSettings.builder()
                        .shopName("Prakash Bankers")
                        .shopAddress("Main Road, Salem")
                        .shopPhone("9876543210")
                        .openingCapital(BigDecimal.ZERO)
                        .openingCapitalSet(false)
                        .build());
            }

            // Preserve existing loan display IDs. If an older migration changed
            // one, reconnect stale notification strings only when the customer
            // and item identify exactly one loan.
            var loans = loanRepository.findAllWithDetails();
            var notifications = notificationRepository.findAll();
            notifications.forEach(notification -> {
                if (loanRepository.findByLoanId(notification.getLoanId()).isPresent()) return;
                var matches = loans.stream()
                        .filter(loan -> loan.getCustomer().getId().equals(notification.getCustomerId()))
                        .filter(loan -> java.util.Objects.equals(loan.getItem(), notification.getItem()))
                        .toList();
                if (matches.size() == 1) {
                    notification.setLoanId(matches.getFirst().getLoanId());
                }
            });
            notificationRepository.saveAll(notifications);
        };
    }
}

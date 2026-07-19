package com.prakashbankers.config;

import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           MaterialRepository materialRepository, AppSettingsRepository appSettingsRepository,
                           RepledgePlaceRepository placeRepository, RepledgerRepository repledgerRepository,
                           LenderRepository lenderRepository, CustomerRepository customerRepository,
                           LoanRepository loanRepository, AppNotificationRepository notificationRepository) {
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

            if (placeRepository.count() == 0) {
                placeRepository.save(RepledgePlace.builder()
                        .name("Muthoot Finance").type(PlaceType.Bank)
                        .contact("1800-313-1212").address("Main Road, Salem")
                        .defaultRate(new BigDecimal("1.5")).build());
                placeRepository.save(RepledgePlace.builder()
                        .name("Sri Ram Jewellers").type(PlaceType.Shop)
                        .contact("044-2434567").address("Big Bazaar St, Salem")
                        .defaultRate(new BigDecimal("1.8")).build());
            }

            if (repledgerRepository.count() == 0) {
                repledgerRepository.save(Repledger.builder().name("Ramesh Kumar").role("Staff").phone("9888877777").build());
                repledgerRepository.save(Repledger.builder().name("Vijay Anand").role("Partner").phone("9877712345").build());
            }

            if (lenderRepository.count() == 0) {
                lenderRepository.save(Lender.builder().name("Selvam Chettiar").phone("9445566778")
                        .address("Market Street, Salem").defaultRate(new BigDecimal("1.2")).build());
            }

            if (customerRepository.count() == 0) {
                Customer c = Customer.builder()
                        .code("CUST-1001")
                        .name("Karthik Raja")
                        .phone("9843211122")
                        .address("12, Gandhi St, Salem")
                        .idProof("AADHAAR 4521 XXXX 8890")
                        .build();

                Loan loan = Loan.builder()
                        .loanId("L-1001")
                        .customer(c)
                        .material("Gold")
                        .item("22K Necklace")
                        .grossWeight(new BigDecimal("32"))
                        .netWeight(new BigDecimal("32"))
                        .purity("22K")
                        .pledgeAmount(new BigDecimal("150000"))
                        .pledgeDate(LocalDate.now().minusMonths(4))
                        .interestType(InterestType.monthly)
                        .interestRate(new BigDecimal("2.0"))
                        .status(LoanStatus.active)
                        .build();

                loan.getTransactions().add(LoanTransaction.builder()
                        .loan(loan).date(LocalDate.now().minusMonths(3))
                        .type(TransactionType.interest).amount(new BigDecimal("3000")).note("Interest payment").build());

                c.getLoans().add(loan);
                customerRepository.save(c);
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

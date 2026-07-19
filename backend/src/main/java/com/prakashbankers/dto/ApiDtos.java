package com.prakashbankers.dto;

import com.prakashbankers.entity.InterestType;
import com.prakashbankers.entity.LoanStatus;
import com.prakashbankers.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ApiDtos {

    private ApiDtos() {}

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String username;
        private String displayName;
        private String role;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private LocalDate date;
        private TransactionType type;
        private BigDecimal amount;
        private String note;
        private BigDecimal rateAtPayment;
        private BigDecimal interestDueAtPayment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoanResponse {
        private Long id;
        private String loanId;
        private String material;
        private String item;
        private BigDecimal grossWeight;
        private BigDecimal netWeight;
        private String purity;
        private BigDecimal pledgeAmount;
        private LocalDate pledgeDate;
        private InterestType interestType;
        private BigDecimal interestRate;
        private LoanStatus status;
        private FinancialSummaryDto financials;
        private RepledgeSummary repledge;
        private List<TransactionResponse> transactions;
        private String materialPhotoFileName;
        private boolean hasMaterialPhoto;
        private String materialPhotoUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RepledgeSummary {
        private Long id;
        private Long placeId;
        private String placeName;
        private Long repledgerId;
        private String repledgerName;
        private LocalDate date;
        private BigDecimal amount;
        private BigDecimal rate;
        private InterestType interestType;
        private FinancialSummaryDto financials;
        private List<TransactionResponse> transactions;
        private BigDecimal rateSpread;
        private FinancialSummaryDto customerFinancials;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerResponse {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private String address;
        private String idProof;
        private String idProofFileName;
        private boolean hasIdProofFile;
        private String idProofUrl;
        private List<LoanResponse> loans;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateCustomerRequest {
        private String name;
        private String phone;
        private String address;
        private String idProof;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerListItem {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private int loanCount;
        private int activeLoanCount;
        private boolean hasOverdue;
        private BigDecimal totalPrincipalDue;
        private BigDecimal totalInterestDue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateCustomerRequest {
        private String name;
        private String phone;
        private String address;
        private String idProof;
        private String material;
        private String item;
        private BigDecimal grossWeight;
        private BigDecimal netWeight;
        private String purity;
        private BigDecimal pledgeAmount;
        private LocalDate pledgeDate;
        private InterestType interestType;
        private BigDecimal interestRate;
        private Boolean prepaidFirstPeriod;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateLoanRequest {
        private String material;
        private String item;
        private BigDecimal grossWeight;
        private BigDecimal netWeight;
        private String purity;
        private BigDecimal pledgeAmount;
        private LocalDate pledgeDate;
        private InterestType interestType;
        private BigDecimal interestRate;
        private Boolean prepaidFirstPeriod;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        private LocalDate date;
        private BigDecimal amount;
        private String note;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RepledgeRequest {
        private Long placeId;
        private Long repledgerId;
        private LocalDate date;
        private BigDecimal amount;
        private BigDecimal rate;
        private InterestType interestType;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationItem {
        private Long id;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String loanId;
        private String item;
        private int overdueCycles;
        private BigDecimal interestBalance;
        private boolean read;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppNotificationResponse {
        private Long id;
        private String loanId;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String item;
        private int overdueCycles;
        private BigDecimal interestBalance;
        private boolean read;
        private java.time.Instant createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationSettingsRequest {
        private String duePreset;
        private Integer customDueDays;
        private boolean pushEnabled;
        private boolean smsEnabled;
        private boolean whatsappEnabled;
        private String smsTemplate;
        private String whatsappTemplate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationSettingsResponse {
        private String duePreset;
        private Integer customDueDays;
        private boolean pushEnabled;
        private boolean smsEnabled;
        private boolean whatsappEnabled;
        private String smsTemplate;
        private String whatsappTemplate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LenderResponse {
        private Long id;
        private String code;
        private String name;
        private String phone;
        private String address;
        private BigDecimal defaultRate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private BigDecimal cashInHandEstimated;
        private BigDecimal inHandBalance;
        private BigDecimal openingCapital;
        private BigDecimal netInterestEarned;
        private int overdueCount;
        private BigDecimal totalActivePrincipal;
        private BigDecimal totalPendingInterest;
        private BigDecimal totalRepledgedPrincipal;
        private BigDecimal totalOwedToBanks;
        private BigDecimal totalBorrowedOutstanding;
        private BigDecimal totalOwedToLenders;
        private BigDecimal goldInVaultWeight;
        private int goldInVaultCount;
        private BigDecimal totalReceivable;
        private BigDecimal totalPayable;
        private BigDecimal netPosition;
        private BigDecimal profitLoss;
        private BigDecimal totalInterestPaidToBanks;
        private List<NotificationItem> attentionList;
        private List<NotificationItem> criticalAlerts;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlaceRequest {
        private String name;
        private String type;
        private String contact;
        private String address;
        private BigDecimal defaultRate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlaceResponse {
        private Long id;
        private String name;
        private String type;
        private String contact;
        private String address;
        private BigDecimal defaultRate;
        private int activeItems;
        private BigDecimal totalLiability;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RepledgerRequest {
        private String name;
        private String role;
        private String phone;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LenderRequest {
        private String name;
        private String phone;
        private String address;
        private BigDecimal defaultRate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BorrowingRequest {
        private Long lenderId;
        private BigDecimal amount;
        private LocalDate date;
        private InterestType interestType;
        private BigDecimal interestRate;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BorrowingResponse {
        private Long id;
        private String borrowingId;
        private Long lenderId;
        private String lenderName;
        private BigDecimal amount;
        private LocalDate date;
        private InterestType interestType;
        private BigDecimal interestRate;
        private String notes;
        private String status;
        private FinancialSummaryDto financials;
        private List<TransactionResponse> transactions;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InHandRequest {
        private String type;
        private BigDecimal amount;
        private LocalDate date;
        private String note;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InHandResponse {
        private Long id;
        private String type;
        private BigDecimal amount;
        private LocalDate date;
        private String note;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InHandSummary {
        private BigDecimal openingCapital;
        private BigDecimal totalPut;
        private BigDecimal totalTake;
        private BigDecimal balance;
        private List<InHandResponse> entries;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SettingsResponse {
        private String shopName;
        private String shopAddress;
        private String shopPhone;
        private BigDecimal openingCapital;
        private boolean openingCapitalSet;
        private List<String> materials;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RepledgeListItem {
        private String loanId;
        private Long customerId;
        private String customerName;
        private String item;
        private BigDecimal customerPrincipal;
        private BigDecimal bankBorrowed;
        private String placeName;
    }
}

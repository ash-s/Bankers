package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import com.prakashbankers.util.FinancialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final LoanRepository loanRepository;
    private final BorrowingRepository borrowingRepository;
    private final InHandEntryRepository inHandEntryRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final InterestEngine interestEngine;

    public DashboardService(LoanRepository loanRepository, BorrowingRepository borrowingRepository,
                            InHandEntryRepository inHandEntryRepository, AppSettingsRepository appSettingsRepository,
                            InterestEngine interestEngine) {
        this.loanRepository = loanRepository;
        this.borrowingRepository = borrowingRepository;
        this.inHandEntryRepository = inHandEntryRepository;
        this.appSettingsRepository = appSettingsRepository;
        this.interestEngine = interestEngine;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        List<Loan> loans = loanRepository.findAllWithDetails();

        BigDecimal totalActivePrincipal = BigDecimal.ZERO;
        BigDecimal totalPendingInterest = BigDecimal.ZERO;
        BigDecimal totalCollectedInt = BigDecimal.ZERO;
        BigDecimal totalCustomerPrincipalCollected = BigDecimal.ZERO;
        BigDecimal totalPledgedOut = BigDecimal.ZERO;
        BigDecimal totalRepledgedPrincipal = BigDecimal.ZERO;
        BigDecimal totalOwedToBanks = BigDecimal.ZERO;
        BigDecimal goldInVaultWeight = BigDecimal.ZERO;
        int goldInVaultCount = 0;

        List<NotificationItem> attention = new ArrayList<>();

        BigDecimal totalInterestPaidToBanks = BigDecimal.ZERO;

        for (Loan loan : loans) {
            FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(loan, today));
            totalCollectedInt = totalCollectedInt.add(fin.getInterestPaid());
            totalCustomerPrincipalCollected = totalCustomerPrincipalCollected.add(fin.getPrincipalPaid());
            totalPledgedOut = totalPledgedOut.add(loan.getPledgeAmount());

            if (loan.getStatus() != LoanStatus.completed) {
                totalActivePrincipal = totalActivePrincipal.add(fin.getCurrentPrincipal());
                totalPendingInterest = totalPendingInterest.add(fin.getInterestBalance());

                if (loan.getStatus() == LoanStatus.active) {
                    goldInVaultWeight = goldInVaultWeight.add(loan.getNetWeight() != null ? loan.getNetWeight() : BigDecimal.ZERO);
                    goldInVaultCount++;
                }

                if (fin.getOverdueCycles() >= 1 && !today.toString().equals(loan.getDismissedNotifDate())) {
                    attention.add(NotificationItem.builder()
                            .customerId(loan.getCustomer().getId())
                            .customerName(loan.getCustomer().getName())
                            .customerPhone(loan.getCustomer().getPhone())
                            .loanId(loan.getLoanId())
                            .item(loan.getItem())
                            .overdueCycles(fin.getOverdueCycles())
                            .interestBalance(fin.getInterestBalance())
                            .build());
                }
            }

            if (loan.getRepledge() != null) {
                Repledge rep = loan.getRepledge();
                FinancialSummaryDto repFin = interestEngine.calculate(
                        FinancialMapper.repledgeInput(rep, loan, today));
                totalRepledgedPrincipal = totalRepledgedPrincipal.add(rep.getAmount());
                totalOwedToBanks = totalOwedToBanks.add(repFin.getCurrentPrincipal()).add(repFin.getInterestBalance());
                totalInterestPaidToBanks = totalInterestPaidToBanks.add(repFin.getInterestPaid());
            }
        }

        List<Borrowing> borrowings = borrowingRepository.findAllWithDetails();
        BigDecimal totalBorrowedOutstanding = BigDecimal.ZERO;
        BigDecimal totalOwedToLenders = BigDecimal.ZERO;
        BigDecimal totalInterestPaidToLenders = BigDecimal.ZERO;
        BigDecimal totalBorrowedReceived = BigDecimal.ZERO;

        for (Borrowing b : borrowings) {
            FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.borrowingInput(b, today));
            totalBorrowedReceived = totalBorrowedReceived.add(b.getAmount());
            totalBorrowedOutstanding = totalBorrowedOutstanding.add(fin.getCurrentPrincipal());
            totalOwedToLenders = totalOwedToLenders.add(fin.getCurrentPrincipal()).add(fin.getInterestBalance());
            totalInterestPaidToLenders = totalInterestPaidToLenders.add(fin.getInterestPaid());
        }

        BigDecimal totalPaidToLenders = totalBorrowedReceived.subtract(totalBorrowedOutstanding).add(totalInterestPaidToLenders);
        BigDecimal cashEstimated = totalBorrowedReceived.add(totalCustomerPrincipalCollected).add(totalCollectedInt)
                .subtract(totalPledgedOut).subtract(totalPaidToLenders);

        AppSettings settings = appSettingsRepository.findAll().stream().findFirst().orElse(new AppSettings());
        BigDecimal opening = settings.getOpeningCapital() != null ? settings.getOpeningCapital() : BigDecimal.ZERO;
        BigDecimal totalPut = sumInHand(InHandType.put);
        BigDecimal totalTake = sumInHand(InHandType.take);
        BigDecimal inHandBalance = opening.add(totalPut).subtract(totalTake);

        attention.sort(Comparator.comparing(NotificationItem::getInterestBalance).reversed());
        List<NotificationItem> critical = attention.stream().filter(n -> n.getOverdueCycles() >= 2).toList();

        BigDecimal totalReceivable = totalActivePrincipal.add(totalPendingInterest);
        BigDecimal totalPayable = totalOwedToBanks.add(totalOwedToLenders);
        BigDecimal netPosition = totalReceivable.subtract(totalPayable);
        BigDecimal profitLoss = totalCollectedInt.subtract(totalInterestPaidToLenders).subtract(totalInterestPaidToBanks);

        return DashboardResponse.builder()
                .cashInHandEstimated(cashEstimated)
                .inHandBalance(inHandBalance)
                .openingCapital(opening)
                .netInterestEarned(totalCollectedInt.subtract(totalInterestPaidToLenders))
                .overdueCount(attention.size())
                .totalActivePrincipal(totalActivePrincipal)
                .totalPendingInterest(totalPendingInterest)
                .totalRepledgedPrincipal(totalRepledgedPrincipal)
                .totalOwedToBanks(totalOwedToBanks)
                .totalBorrowedOutstanding(totalBorrowedOutstanding)
                .totalOwedToLenders(totalOwedToLenders)
                .goldInVaultWeight(goldInVaultWeight)
                .goldInVaultCount(goldInVaultCount)
                .totalReceivable(totalReceivable)
                .totalPayable(totalPayable)
                .netPosition(netPosition)
                .profitLoss(profitLoss)
                .totalInterestPaidToBanks(totalInterestPaidToBanks)
                .attentionList(attention.stream().limit(8).toList())
                .criticalAlerts(critical)
                .build();
    }

    private BigDecimal sumInHand(InHandType type) {
        return inHandEntryRepository.findByType(type).stream()
                .map(InHandEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

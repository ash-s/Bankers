package com.prakashbankers.service;

import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.dto.InterestCalcInput;
import com.prakashbankers.dto.InterestBreakdownDto;
import com.prakashbankers.dto.LedgerTransactionDto;
import com.prakashbankers.entity.InterestType;
import com.prakashbankers.entity.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InterestEngine {

    private static final int SCALE = 2;
    private static final RoundingMode ROUND = RoundingMode.HALF_UP;

    public FinancialSummaryDto calculate(InterestCalcInput input) {
        BigDecimal principal = nz(input.getPrincipal());
        LocalDate startDate = input.getStartDate();
        BigDecimal rate = nz(input.getRate());
        InterestType interestType = input.getInterestType() != null ? input.getInterestType() : InterestType.monthly;
        LocalDate calcDate = input.getCalcDate() != null ? input.getCalcDate() : LocalDate.now();

        if (startDate == null) {
            throw new IllegalArgumentException("Interest start date is required");
        }

        // Future-dated transactions must not alter today's balance.
        List<LedgerTransactionDto> txs = new ArrayList<>(input.getTransactions() != null
                ? input.getTransactions().stream()
                    .filter(t -> t.getDate() != null)
                    .filter(t -> !t.getDate().isAfter(calcDate))
                    .filter(t -> !t.getDate().isBefore(startDate))
                    .toList()
                : List.of());
        txs.sort(Comparator.comparing(LedgerTransactionDto::getDate));

        BigDecimal principalPaid = sumByType(txs, TransactionType.principal);
        BigDecimal interestPaid = sumByType(txs, TransactionType.interest);
        BigDecimal discountGiven = sumByType(txs, TransactionType.discount);

        List<Period> periods = buildPrincipalPeriods(principal, startDate, txs);
        List<InterestBreakdownDto> breakdown = new ArrayList<>();

        BigDecimal interestGenerated = BigDecimal.ZERO;
        for (Period p : periods) {
            LocalDate end = p.endDate != null ? p.endDate : calcDate;
            if (!p.startDate.isBefore(end) || p.principal.compareTo(BigDecimal.ZERO) <= 0) continue;
            interestGenerated = interestGenerated.add(
                    calculatePeriod(startDate, p.startDate, end, p.principal, rate, interestType, breakdown));
        }

        interestGenerated = interestGenerated.setScale(SCALE, ROUND);
        BigDecimal currentPrincipal = principal.subtract(principalPaid).max(BigDecimal.ZERO).setScale(SCALE, ROUND);
        BigDecimal rawBalance = interestGenerated.subtract(interestPaid).subtract(discountGiven);
        BigDecimal interestBalance = rawBalance.max(BigDecimal.ZERO).setScale(SCALE, ROUND);
        BigDecimal advanceInterestCredit = rawBalance.min(BigDecimal.ZERO).abs().setScale(SCALE, ROUND);

        BigDecimal cycleAmt = cycleInterest(currentPrincipal, rate, interestType);
        int overdueCycles = 0;
        if (currentPrincipal.compareTo(BigDecimal.ZERO) > 0 && cycleAmt.compareTo(BigDecimal.ZERO) > 0) {
            overdueCycles = interestBalance.divide(cycleAmt, 0, RoundingMode.FLOOR).intValue();
        }

        return FinancialSummaryDto.builder()
            .currentPrincipal(currentPrincipal)
            .principalPaid(principalPaid.setScale(SCALE, ROUND))
            .interestGenerated(interestGenerated)
            .interestPaid(interestPaid.setScale(SCALE, ROUND))
            .discountGiven(discountGiven.setScale(SCALE, ROUND))
            .interestBalance(interestBalance)
            .advanceInterestCredit(advanceInterestCredit)
            .cycleInterestAmount(cycleAmt)
            .overdueCycles(overdueCycles)
            .asOfDate(calcDate)
            .interestBreakdown(breakdown)
            .build();
    }

    public BigDecimal cycleInterest(BigDecimal principal, BigDecimal rate, InterestType type) {
        if (principal.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO.setScale(SCALE, ROUND);
        BigDecimal r = rate.divide(BigDecimal.valueOf(100), 10, ROUND);
        BigDecimal amt = type == InterestType.monthly
            ? principal.multiply(r)
            : principal.multiply(r).multiply(BigDecimal.valueOf(30));
        return amt.setScale(SCALE, ROUND);
    }

    /** One complete calendar-month cycle, or 30 daily-rate days. */
    public BigDecimal firstPeriodInterest(BigDecimal principal, BigDecimal rate, InterestType type) {
        return cycleInterest(principal, rate, type);
    }

    private BigDecimal calculatePeriod(
            LocalDate anchor,
            LocalDate from,
            LocalDate to,
            BigDecimal principal,
            BigDecimal rate,
            InterestType type,
            List<InterestBreakdownDto> breakdown) {
        BigDecimal rateFraction = rate.divide(BigDecimal.valueOf(100), 12, ROUND);

        if (type == InterestType.daily) {
            long days = ChronoUnit.DAYS.between(from, to);
            BigDecimal interest = principal.multiply(rateFraction).multiply(BigDecimal.valueOf(days));
            breakdown.add(breakdown(from, to, principal, rate, type, days, 1, interest));
            return interest;
        }

        BigDecimal total = BigDecimal.ZERO;
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            CalendarCycle cycle = cycleContaining(anchor, cursor);
            LocalDate segmentEnd = to.isBefore(cycle.end) ? to : cycle.end;
            long segmentDays = ChronoUnit.DAYS.between(cursor, segmentEnd);
            long cycleDays = ChronoUnit.DAYS.between(cycle.start, cycle.end);

            BigDecimal interest = principal.multiply(rateFraction)
                    .multiply(BigDecimal.valueOf(segmentDays))
                    .divide(BigDecimal.valueOf(cycleDays), 12, ROUND);
            total = total.add(interest);
            breakdown.add(breakdown(cursor, segmentEnd, principal, rate, type,
                    segmentDays, cycleDays, interest));
            cursor = segmentEnd;
        }
        return total;
    }

    private InterestBreakdownDto breakdown(
            LocalDate from,
            LocalDate to,
            BigDecimal principal,
            BigDecimal rate,
            InterestType type,
            long days,
            long cycleDays,
            BigDecimal interest) {
        return InterestBreakdownDto.builder()
                .fromDate(from)
                .toDate(to)
                .principal(principal.setScale(SCALE, ROUND))
                .rate(rate)
                .interestType(type)
                .days(days)
                .cycleDays(cycleDays)
                .interest(interest.setScale(SCALE, ROUND))
                .build();
    }

    private CalendarCycle cycleContaining(LocalDate anchor, LocalDate date) {
        long monthGuess = ChronoUnit.MONTHS.between(
                anchor.withDayOfMonth(1), date.withDayOfMonth(1));
        long cycleIndex = Math.max(0, monthGuess);

        while (anniversary(anchor, cycleIndex).isAfter(date)) {
            cycleIndex--;
        }
        while (!anniversary(anchor, cycleIndex + 1).isAfter(date)) {
            cycleIndex++;
        }
        return new CalendarCycle(
                anniversary(anchor, cycleIndex),
                anniversary(anchor, cycleIndex + 1));
    }

    private LocalDate anniversary(LocalDate anchor, long months) {
        return anchor.plusMonths(months);
    }

    public void validatePayment(TransactionType type, BigDecimal amount, FinancialSummaryDto fin) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        if (type == TransactionType.principal) {
            if (amount.compareTo(fin.getCurrentPrincipal()) > 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Amount exceeds outstanding principal of " + fin.getCurrentPrincipal());
            }
        } else if (type == TransactionType.interest || type == TransactionType.discount) {
            if (amount.compareTo(fin.getInterestBalance()) > 0) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Amount exceeds interest due of " + fin.getInterestBalance());
            }
        }
    }

    public void validatePaymentDate(LocalDate paymentDate, LocalDate accountStartDate) {
        if (paymentDate.isBefore(accountStartDate)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Payment date cannot be before the account start date");
        }
    }

    private List<Period> buildPrincipalPeriods(BigDecimal initialPrincipal, LocalDate startDate, List<LedgerTransactionDto> txs) {
        List<Period> periods = new ArrayList<>();
        BigDecimal balance = initialPrincipal;
        LocalDate periodStart = startDate;

        for (LedgerTransactionDto tx : txs) {
            if (tx.getType() == TransactionType.principal) {
                periods.add(new Period(periodStart, tx.getDate(), balance));
                balance = balance.subtract(nz(tx.getAmount())).max(BigDecimal.ZERO);
                periodStart = tx.getDate();
            }
        }
        periods.add(new Period(periodStart, null, balance));
        return periods;
    }

    private BigDecimal sumByType(List<LedgerTransactionDto> txs, TransactionType type) {
        return txs.stream()
            .filter(t -> t.getType() == type)
            .map(t -> nz(t.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class Period {
        final LocalDate startDate;
        final LocalDate endDate;
        final BigDecimal principal;

        Period(LocalDate startDate, LocalDate endDate, BigDecimal principal) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.principal = principal;
        }
    }

    private record CalendarCycle(LocalDate start, LocalDate end) {}
}

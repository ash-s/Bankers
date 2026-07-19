package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.BorrowingRepository;
import com.prakashbankers.repository.LenderRepository;
import com.prakashbankers.util.FinancialMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final LenderRepository lenderRepository;
    private final InterestEngine interestEngine;

    public BorrowingService(BorrowingRepository borrowingRepository, LenderRepository lenderRepository,
                            InterestEngine interestEngine) {
        this.borrowingRepository = borrowingRepository;
        this.lenderRepository = lenderRepository;
        this.interestEngine = interestEngine;
    }

    @Transactional(readOnly = true)
    public List<BorrowingResponse> listBorrowings(String search) {
        return borrowingRepository.findAllWithDetails().stream()
                .filter(b -> search == null || search.isBlank()
                        || b.getLender().getName().toLowerCase().contains(search.toLowerCase())
                        || (b.getNotes() != null && b.getNotes().toLowerCase().contains(search.toLowerCase())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BorrowingResponse getBorrowing(Long id) {
        Borrowing b = borrowingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrowing not found"));
        return toResponse(b);
    }

    @Transactional
    public BorrowingResponse create(BorrowingRequest req) {
        Lender lender = lenderRepository.findById(req.getLenderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lender not found"));
        Borrowing b = Borrowing.builder()
                .borrowingId(FinancialMapper.temporaryCode("BRW"))
                .lender(lender)
                .amount(req.getAmount())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .interestType(req.getInterestType() != null ? req.getInterestType() : InterestType.monthly)
                .interestRate(req.getInterestRate())
                .notes(req.getNotes())
                .status(BorrowingStatus.active)
                .build();
        borrowingRepository.saveAndFlush(b);
        b.setBorrowingId(FinancialMapper.orderedCode("BRW", b.getId()));
        return toResponse(borrowingRepository.save(b));
    }

    @Transactional
    public BorrowingResponse addPayment(Long id, TransactionType type, PaymentRequest req) {
        Borrowing b = borrowingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrowing not found"));
        LocalDate payDate = req.getDate() != null ? req.getDate() : LocalDate.now();
        interestEngine.validatePaymentDate(payDate, b.getDate());
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.borrowingInput(b, payDate));
        interestEngine.validatePayment(type, req.getAmount(), fin);

        BorrowingTransaction tx = BorrowingTransaction.builder()
                .borrowing(b)
                .date(payDate)
                .type(type)
                .amount(req.getAmount())
                .note(req.getNote())
                .build();

        if (type == TransactionType.principal) {
            tx.setRateAtPayment(b.getInterestRate());
            tx.setInterestDueAtPayment(fin.getInterestBalance());
        }

        b.getTransactions().add(tx);
        borrowingRepository.save(b);
        return toResponse(b);
    }

    @Transactional
    public BorrowingResponse close(Long id) {
        Borrowing b = borrowingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrowing not found"));
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.borrowingInput(b, LocalDate.now()));
        if (fin.getCurrentPrincipal().compareTo(BigDecimal.ZERO) > 0
                || fin.getInterestBalance().compareTo(new BigDecimal("0.50")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outstanding balance remains");
        }
        b.setStatus(BorrowingStatus.closed);
        return toResponse(borrowingRepository.save(b));
    }

    private BorrowingResponse toResponse(Borrowing b) {
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.borrowingInput(b, LocalDate.now()));
        return BorrowingResponse.builder()
                .id(b.getId())
                .borrowingId(b.getBorrowingId())
                .lenderId(b.getLender().getId())
                .lenderName(b.getLender().getName())
                .amount(b.getAmount())
                .date(b.getDate())
                .interestType(b.getInterestType())
                .interestRate(b.getInterestRate())
                .notes(b.getNotes())
                .status(b.getStatus().name())
                .financials(fin)
                .transactions(b.getTransactions().stream().map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .date(t.getDate())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .note(t.getNote())
                        .rateAtPayment(t.getRateAtPayment())
                        .interestDueAtPayment(t.getInterestDueAtPayment())
                        .build()).toList())
                .build();
    }
}

package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import com.prakashbankers.util.FinancialMapper;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final RepledgePlaceRepository placeRepository;
    private final RepledgerRepository repledgerRepository;
    private final InterestEngine interestEngine;
    private final Path uploadDir;

    public CustomerService(CustomerRepository customerRepository, LoanRepository loanRepository,
                           RepledgePlaceRepository placeRepository, RepledgerRepository repledgerRepository,
                           InterestEngine interestEngine, @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.placeRepository = placeRepository;
        this.repledgerRepository = repledgerRepository;
        this.interestEngine = interestEngine;
        this.uploadDir = Paths.get(uploadDir);
        Files.createDirectories(this.uploadDir);
    }

    @Transactional(readOnly = true)
    public List<CustomerListItem> listCustomers(String search, String filter) {
        LocalDate today = LocalDate.now();
        List<Customer> customers = (search == null || search.isBlank())
                ? customerRepository.findAllWithLoans()
                : customerRepository.findByNameContainingIgnoreCaseOrPhoneContaining(search.trim(), search.trim());

        return customers.stream()
                .filter(c -> matchesFilter(c, filter, today))
                .map(c -> toListItem(c, today))
                .toList();
    }

    private boolean matchesFilter(Customer c, String filter, LocalDate today) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) return true;
        return switch (filter.toLowerCase()) {
            case "overdue" -> c.getLoans().stream()
                    .filter(l -> l.getStatus() != LoanStatus.completed)
                    .anyMatch(l -> interestEngine.calculate(FinancialMapper.loanInput(l, today)).getOverdueCycles() >= 1);
            case "active" -> c.getLoans().stream().anyMatch(l -> l.getStatus() != LoanStatus.completed);
            case "vault" -> c.getLoans().stream().anyMatch(l -> l.getStatus() == LoanStatus.active);
            case "repledged" -> c.getLoans().stream().anyMatch(l -> l.getStatus() == LoanStatus.repledged);
            default -> true;
        };
    }

    private CustomerListItem toListItem(Customer c, LocalDate today) {
        int activeCount = 0;
        boolean hasOverdue = false;
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (Loan l : c.getLoans()) {
            if (l.getStatus() == LoanStatus.completed) continue;
            activeCount++;
            FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(l, today));
            totalPrincipal = totalPrincipal.add(fin.getCurrentPrincipal());
            totalInterest = totalInterest.add(fin.getInterestBalance());
            if (fin.getOverdueCycles() >= 1) hasOverdue = true;
        }

        return CustomerListItem.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .phone(c.getPhone())
                .loanCount(c.getLoans().size())
                .activeLoanCount(activeCount)
                .hasOverdue(hasOverdue)
                .totalPrincipalDue(totalPrincipal)
                .totalInterestDue(totalInterest)
                .build();
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest req) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        if (req.getName() != null) c.setName(req.getName());
        if (req.getPhone() != null) c.setPhone(blankToNull(req.getPhone()));
        if (req.getAddress() != null) c.setAddress(req.getAddress());
        if (req.getIdProof() != null) c.setIdProof(req.getIdProof());
        customerRepository.save(c);
        return getCustomer(id);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer c = customerRepository.findByIdWithLoans(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        boolean hasActive = c.getLoans().stream().anyMatch(l -> l.getStatus() != LoanStatus.completed);
        if (hasActive) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete customer with active loans. Close all loans first.");
        }
        customerRepository.delete(c);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        Customer c = customerRepository.findByIdWithLoans(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        hydrateLoans(c);
        return toCustomerResponse(c);
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest req, MultipartFile idProofFile, MultipartFile materialPhoto) {
        Customer customer = Customer.builder()
                .code(FinancialMapper.temporaryCode("CUST"))
                .name(req.getName())
                .phone(blankToNull(req.getPhone()))
                .address(req.getAddress())
                .idProof(req.getIdProof())
                .build();

        if (idProofFile != null && !idProofFile.isEmpty()) {
            saveIdProof(customer, idProofFile);
        }

        Loan loan = buildLoan(customer, req.getMaterial(), req.getItem(), req.getGrossWeight(), req.getNetWeight(),
                req.getPurity(), req.getPledgeAmount(), req.getPledgeDate(), req.getInterestType(), req.getInterestRate(),
                req.getPrepaidFirstPeriod());
        if (materialPhoto != null && !materialPhoto.isEmpty()) {
            saveMaterialPhoto(loan, materialPhoto);
        }
        customer.getLoans().add(loan);
        customerRepository.saveAndFlush(customer);
        customer.setCode(FinancialMapper.orderedCode("CUST", customer.getId()));
        loan.setLoanId(FinancialMapper.orderedCode("LN", loan.getId()));
        customerRepository.saveAndFlush(customer);
        return getCustomer(customer.getId());
    }

    @Transactional
    public CustomerResponse addLoan(Long customerId, CreateLoanRequest req, MultipartFile materialPhoto) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Loan loan = buildLoan(c, req.getMaterial(), req.getItem(), req.getGrossWeight(), req.getNetWeight(),
                req.getPurity(), req.getPledgeAmount(), req.getPledgeDate(), req.getInterestType(), req.getInterestRate(),
                req.getPrepaidFirstPeriod());
        if (materialPhoto != null && !materialPhoto.isEmpty()) {
            saveMaterialPhoto(loan, materialPhoto);
        }
        // Persist the loan directly. Saving it through the existing customer merge-cascades onto
        // the transient loan and persists a *copy*, leaving this instance without an ID, which
        // made orderedCode throw "Saved entity ID is required" (HTTP 500).
        loanRepository.saveAndFlush(loan);
        loan.setLoanId(FinancialMapper.orderedCode("LN", loan.getId()));
        c.getLoans().add(loan);
        customerRepository.saveAndFlush(c);
        return getCustomer(customerId);
    }

    @Transactional
    public CustomerResponse addPayment(String loanId, TransactionType type, PaymentRequest req) {
        Loan loan = loadLoanFull(loanId);
        if (loan.getStatus() == LoanStatus.completed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loan is closed");
        }

        LocalDate payDate = req.getDate() != null ? req.getDate() : LocalDate.now();
        interestEngine.validatePaymentDate(payDate, loan.getPledgeDate());
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(loan, payDate));
        interestEngine.validatePayment(type, req.getAmount(), fin);

        LoanTransaction.LoanTransactionBuilder txBuilder = LoanTransaction.builder()
                .loan(loan)
                .date(payDate)
                .type(type)
                .amount(req.getAmount())
                .note(req.getNote());

        if (type == TransactionType.principal) {
            txBuilder.rateAtPayment(loan.getInterestRate())
                    .interestDueAtPayment(fin.getInterestBalance());
        }

        loan.getTransactions().add(txBuilder.build());
        loanRepository.save(loan);
        return getCustomer(loan.getCustomer().getId());
    }

    @Transactional
    public CustomerResponse applyDiscount(String loanId, PaymentRequest req) {
        return addPayment(loanId, TransactionType.discount, req);
    }

    @Transactional
    public CustomerResponse configureRepledge(String loanId, RepledgeRequest req) {
        Loan loan = loadLoanFull(loanId);
        RepledgePlace place = placeRepository.findById(req.getPlaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));
        Repledger repledger = repledgerRepository.findById(req.getRepledgerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repledger not found"));

        Repledge existing = loan.getRepledge();
        Repledge repledge = existing != null ? existing : Repledge.builder().loan(loan).transactions(new java.util.ArrayList<>()).build();
        repledge.setPlace(place);
        repledge.setRepledger(repledger);
        repledge.setDate(req.getDate());
        repledge.setAmount(req.getAmount());
        repledge.setRate(req.getRate());
        repledge.setInterestType(req.getInterestType());
        loan.setRepledge(repledge);
        loan.setStatus(LoanStatus.repledged);
        loanRepository.save(loan);
        return getCustomer(loan.getCustomer().getId());
    }

    @Transactional
    public CustomerResponse removeRepledge(String loanId) {
        Loan loan = loadLoanFull(loanId);
        loan.setRepledge(null);
        loan.setStatus(LoanStatus.active);
        loanRepository.save(loan);
        return getCustomer(loan.getCustomer().getId());
    }

    @Transactional
    public CustomerResponse closeLoan(String loanId) {
        Loan loan = loadLoanFull(loanId);
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(loan, LocalDate.now()));
        if (fin.getCurrentPrincipal().compareTo(BigDecimal.ZERO) > 0
                || fin.getInterestBalance().compareTo(new BigDecimal("0.50")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot close: outstanding principal or interest remains");
        }
        loan.setStatus(LoanStatus.completed);
        loan.setRepledge(null);
        loanRepository.save(loan);
        return getCustomer(loan.getCustomer().getId());
    }

    @Transactional
    public void dismissNotification(String loanId) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        loan.setDismissedNotifDate(LocalDate.now().toString());
        loanRepository.save(loan);
    }

    public Path resolveIdProofPath(Long customerId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        if (c.getIdProofFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No ID proof file");
        }
        return uploadDir.resolve(c.getIdProofFilePath());
    }

    public Path resolveMaterialPhotoPath(String loanId) {
        Loan loan = loanRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        if (loan.getMaterialPhotoFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No material photo");
        }
        return uploadDir.resolve(loan.getMaterialPhotoFilePath());
    }

    @Transactional(readOnly = true)
    public CustomerResponse findCustomerByLoanId(String loanId) {
        Loan loan = loanRepository.findByLoanIdWithCustomerAndRepledge(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        Customer c = loan.getCustomer();
        Hibernate.initialize(c.getLoans());
        hydrateLoans(c);
        return toCustomerResponse(c);
    }

    private Loan loadLoanFull(String loanId) {
        Loan loan = loanRepository.findByLoanIdWithCustomerAndRepledge(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        Hibernate.initialize(loan.getTransactions());
        if (loan.getRepledge() != null) {
            Hibernate.initialize(loan.getRepledge().getTransactions());
        }
        return loan;
    }

    private void hydrateLoans(Customer c) {
        for (Loan l : c.getLoans()) {
            Hibernate.initialize(l.getTransactions());
            if (l.getRepledge() != null) {
                Hibernate.initialize(l.getRepledge().getTransactions());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> getNotifications() {
        return loanRepository.findAllWithDetails().stream()
                .filter(l -> l.getStatus() != LoanStatus.completed)
                .map(l -> {
                    FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(l, LocalDate.now()));
                    if (fin.getOverdueCycles() < 1 || LocalDate.now().toString().equals(l.getDismissedNotifDate())) {
                        return null;
                    }
                    return NotificationItem.builder()
                            .customerId(l.getCustomer().getId())
                            .customerName(l.getCustomer().getName())
                            .customerPhone(l.getCustomer().getPhone())
                            .loanId(l.getLoanId())
                            .item(l.getItem())
                            .overdueCycles(fin.getOverdueCycles())
                            .interestBalance(fin.getInterestBalance())
                            .build();
                })
                .filter(n -> n != null)
                .toList();
    }

    private Loan buildLoan(Customer customer, String material, String item, BigDecimal gross, BigDecimal net,
                           String purity, BigDecimal amount, LocalDate date, InterestType type, BigDecimal rate,
                           Boolean prepaidFirstPeriod) {
        InterestType interestType = type != null ? type : InterestType.monthly;
        LocalDate pledgeDate = date != null ? date : LocalDate.now();
        Loan loan = Loan.builder()
                .loanId(FinancialMapper.temporaryCode("LN"))
                .customer(customer)
                .material(material)
                .item(item)
                .grossWeight(gross)
                .netWeight(net)
                .purity(purity)
                .pledgeAmount(amount)
                .pledgeDate(pledgeDate)
                .interestType(interestType)
                .interestRate(rate)
                .status(LoanStatus.active)
                .transactions(new java.util.ArrayList<>())
                .build();

        if (Boolean.TRUE.equals(prepaidFirstPeriod)) {
            BigDecimal firstPeriod = interestEngine.firstPeriodInterest(amount, rate, interestType);
            loan.getTransactions().add(LoanTransaction.builder()
                    .loan(loan)
                    .date(pledgeDate)
                    .type(TransactionType.interest)
                    .amount(firstPeriod)
                    .note("First period interest prepaid at pledge")
                    .build());
        }
        return loan;
    }

    private void saveIdProof(Customer customer, MultipartFile file) {
        String filename = storeFile(file);
        customer.setIdProofFileName(file.getOriginalFilename());
        customer.setIdProofFilePath(filename);
        customer.setIdProofMimeType(file.getContentType());
    }

    private void saveMaterialPhoto(Loan loan, MultipartFile file) {
        String filename = storeFile(file);
        loan.setMaterialPhotoFileName(file.getOriginalFilename());
        loan.setMaterialPhotoFilePath(filename);
        loan.setMaterialPhotoMimeType(file.getContentType());
    }

    private String storeFile(MultipartFile file) {
        try {
            String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadDir.resolve(filename));
            return filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file");
        }
    }

    private CustomerResponse toCustomerResponse(Customer c) {
        LocalDate today = LocalDate.now();
        List<LoanResponse> loans = c.getLoans().stream().map(l -> toLoanResponse(l, today)).toList();
        return CustomerResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .phone(c.getPhone())
                .address(c.getAddress())
                .idProof(c.getIdProof())
                .idProofFileName(c.getIdProofFileName())
                .hasIdProofFile(c.getIdProofFilePath() != null)
                .idProofUrl(c.getIdProofFilePath() != null ? "/api/customers/" + c.getId() + "/id-proof" : null)
                .loans(loans)
                .build();
    }

    public LoanResponse toLoanResponse(Loan l, LocalDate today) {
        FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(l, today));
        RepledgeSummary repSummary = null;
        if (l.getRepledge() != null) {
            Repledge r = l.getRepledge();
            FinancialSummaryDto repFin = interestEngine.calculate(FinancialMapper.repledgeInput(r, l, today));
            repSummary = RepledgeSummary.builder()
                    .id(r.getId())
                    .placeId(r.getPlace().getId())
                    .placeName(r.getPlace().getName())
                    .repledgerId(r.getRepledger().getId())
                    .repledgerName(r.getRepledger().getName())
                    .date(r.getDate())
                    .amount(r.getAmount())
                    .rate(r.getRate())
                    .interestType(r.getInterestType() != null ? r.getInterestType() : l.getInterestType())
                    .financials(repFin)
                    .customerFinancials(fin)
                    .rateSpread(l.getInterestRate().subtract(r.getRate()))
                    .transactions(r.getTransactions().stream().map(this::toTx).toList())
                    .build();
        }
        return LoanResponse.builder()
                .id(l.getId())
                .loanId(l.getLoanId())
                .material(l.getMaterial())
                .item(l.getItem())
                .grossWeight(l.getGrossWeight())
                .netWeight(l.getNetWeight())
                .purity(l.getPurity())
                .pledgeAmount(l.getPledgeAmount())
                .pledgeDate(l.getPledgeDate())
                .interestType(l.getInterestType())
                .interestRate(l.getInterestRate())
                .status(l.getStatus())
                .financials(fin)
                .repledge(repSummary)
                .transactions(l.getTransactions().stream().map(this::toTx).toList())
                .materialPhotoFileName(l.getMaterialPhotoFileName())
                .hasMaterialPhoto(l.getMaterialPhotoFilePath() != null)
                .materialPhotoUrl(l.getMaterialPhotoFilePath() != null ? "/api/loans/" + l.getLoanId() + "/material-photo" : null)
                .build();
    }

    private TransactionResponse toTx(LoanTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .date(t.getDate())
                .type(t.getType())
                .amount(t.getAmount())
                .note(t.getNote())
                .rateAtPayment(t.getRateAtPayment())
                .interestDueAtPayment(t.getInterestDueAtPayment())
                .build();
    }

    private TransactionResponse toTx(RepledgeTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .date(t.getDate())
                .type(t.getType())
                .amount(t.getAmount())
                .rateAtPayment(t.getRateAtPayment())
                .interestDueAtPayment(t.getInterestDueAtPayment())
                .build();
    }

    /** Optional fields: treat blank input as "not provided" so we store NULL, not "". */
    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

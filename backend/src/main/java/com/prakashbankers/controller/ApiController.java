package com.prakashbankers.controller;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.entity.TransactionType;
import com.prakashbankers.service.BillPdfService;
import com.prakashbankers.service.CustomerService;
import com.prakashbankers.service.DashboardService;
import com.prakashbankers.service.NotificationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DashboardService dashboardService;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final BillPdfService billPdfService;

    public ApiController(DashboardService dashboardService, CustomerService customerService,
                         NotificationService notificationService, BillPdfService billPdfService) {
        this.dashboardService = dashboardService;
        this.customerService = customerService;
        this.notificationService = notificationService;
        this.billPdfService = billPdfService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }

    @PostMapping("/notifications/sync")
    public Map<String, Object> syncNotifications() {
        notificationService.syncOnStartup();
        return Map.of("synced", true, "count", notificationService.unreadCount());
    }

    @GetMapping("/notifications")
    public List<AppNotificationResponse> notifications() {
        return notificationService.listNotifications();
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount());
    }

    @PostMapping("/notifications/{id}/read")
    public void markNotificationRead(@PathVariable Long id) {
        notificationService.markRead(id);
    }

    @PostMapping("/notifications/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @GetMapping("/notification-settings")
    public NotificationSettingsResponse notificationSettings() {
        return notificationService.getSettings();
    }

    @PutMapping("/notification-settings")
    public NotificationSettingsResponse updateNotificationSettings(@RequestBody NotificationSettingsRequest req) {
        return notificationService.updateSettings(req);
    }

    @GetMapping("/customers")
    public List<CustomerListItem> customers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter) {
        return customerService.listCustomers(search, filter);
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse customer(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

    @PutMapping("/customers/{id}")
    public CustomerResponse updateCustomer(@PathVariable Long id, @RequestBody UpdateCustomerRequest req) {
        return customerService.updateCustomer(id, req);
    }

    @DeleteMapping("/customers/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }

    @PostMapping(value = "/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomerResponse createCustomerMultipart(
            @RequestParam String name,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String idProof,
            @RequestParam String material,
            @RequestParam String item,
            @RequestParam java.math.BigDecimal grossWeight,
            @RequestParam java.math.BigDecimal netWeight,
            @RequestParam String purity,
            @RequestParam java.math.BigDecimal pledgeAmount,
            @RequestParam java.time.LocalDate pledgeDate,
            @RequestParam String interestType,
            @RequestParam java.math.BigDecimal interestRate,
            @RequestParam(value = "prepaidFirstPeriod", defaultValue = "false") boolean prepaidFirstPeriod,
            @RequestParam(value = "idProofFile", required = false) MultipartFile idProofFile,
            @RequestParam(value = "materialPhoto", required = false) MultipartFile materialPhoto) {
        CreateCustomerRequest data = CreateCustomerRequest.builder()
                .name(name).phone(phone).address(address).idProof(idProof)
                .material(material).item(item).grossWeight(grossWeight).netWeight(netWeight)
                .purity(purity).pledgeAmount(pledgeAmount).pledgeDate(pledgeDate)
                .interestType(com.prakashbankers.entity.InterestType.valueOf(interestType))
                .interestRate(interestRate)
                .prepaidFirstPeriod(prepaidFirstPeriod)
                .build();
        return customerService.createCustomer(data, idProofFile, materialPhoto);
    }

    @PostMapping(value = "/customers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CustomerResponse createCustomerJson(@RequestBody CreateCustomerRequest data) {
        return customerService.createCustomer(data, null, null);
    }

    @PostMapping(value = "/customers/{id}/loans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomerResponse addLoanMultipart(
            @PathVariable Long id,
            @RequestParam String material,
            @RequestParam String item,
            @RequestParam java.math.BigDecimal grossWeight,
            @RequestParam java.math.BigDecimal netWeight,
            @RequestParam String purity,
            @RequestParam java.math.BigDecimal pledgeAmount,
            @RequestParam java.time.LocalDate pledgeDate,
            @RequestParam String interestType,
            @RequestParam java.math.BigDecimal interestRate,
            @RequestParam(value = "prepaidFirstPeriod", defaultValue = "false") boolean prepaidFirstPeriod,
            @RequestParam(value = "materialPhoto", required = false) MultipartFile materialPhoto) {
        CreateLoanRequest req = CreateLoanRequest.builder()
                .material(material).item(item).grossWeight(grossWeight).netWeight(netWeight)
                .purity(purity).pledgeAmount(pledgeAmount).pledgeDate(pledgeDate)
                .interestType(com.prakashbankers.entity.InterestType.valueOf(interestType))
                .interestRate(interestRate)
                .prepaidFirstPeriod(prepaidFirstPeriod)
                .build();
        return customerService.addLoan(id, req, materialPhoto);
    }

    @PostMapping(value = "/customers/{id}/loans", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CustomerResponse addLoanJson(@PathVariable Long id, @RequestBody CreateLoanRequest req) {
        return customerService.addLoan(id, req, null);
    }

    @GetMapping("/customers/{id}/id-proof")
    public ResponseEntity<Resource> downloadIdProof(@PathVariable Long id) throws Exception {
        Path path = customerService.resolveIdProofPath(id);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/loans/{loanId}/material-photo")
    public ResponseEntity<Resource> downloadMaterialPhoto(@PathVariable String loanId) throws Exception {
        Path path = customerService.resolveMaterialPhotoPath(loanId);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/loans/{loanId}/bill.pdf")
    public ResponseEntity<byte[]> downloadBill(@PathVariable String loanId) {
        CustomerResponse c = customerService.findCustomerByLoanId(loanId);
        byte[] pdf = billPdfService.generateBill(c, loanId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bill-" + loanId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/loans/{loanId}/payments/principal")
    public CustomerResponse payPrincipal(@PathVariable String loanId, @RequestBody PaymentRequest req) {
        return customerService.addPayment(loanId, TransactionType.principal, req);
    }

    @PostMapping("/loans/{loanId}/payments/interest")
    public CustomerResponse payInterest(@PathVariable String loanId, @RequestBody PaymentRequest req) {
        return customerService.addPayment(loanId, TransactionType.interest, req);
    }

    @PostMapping("/loans/{loanId}/payments/discount")
    public CustomerResponse applyDiscount(@PathVariable String loanId, @RequestBody PaymentRequest req) {
        return customerService.applyDiscount(loanId, req);
    }

    @PostMapping("/loans/{loanId}/repledge")
    public CustomerResponse repledge(@PathVariable String loanId, @RequestBody RepledgeRequest req) {
        return customerService.configureRepledge(loanId, req);
    }

    @DeleteMapping("/loans/{loanId}/repledge")
    public CustomerResponse removeRepledge(@PathVariable String loanId) {
        return customerService.removeRepledge(loanId);
    }

    @PostMapping("/loans/{loanId}/close")
    public CustomerResponse closeLoan(@PathVariable String loanId) {
        return customerService.closeLoan(loanId);
    }

    @PostMapping("/loans/{loanId}/dismiss-notification")
    public void dismissNotification(@PathVariable String loanId) {
        customerService.dismissNotification(loanId);
    }
}

package com.prakashbankers.controller;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.entity.Repledger;
import com.prakashbankers.entity.TransactionType;
import com.prakashbankers.service.BorrowingService;
import com.prakashbankers.service.MasterDataService;
import com.prakashbankers.service.RepledgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MasterApiController {

    private final MasterDataService masterDataService;
    private final RepledgeService repledgeService;
    private final BorrowingService borrowingService;

    public MasterApiController(MasterDataService masterDataService, RepledgeService repledgeService,
                               BorrowingService borrowingService) {
        this.masterDataService = masterDataService;
        this.repledgeService = repledgeService;
        this.borrowingService = borrowingService;
    }

    @GetMapping("/places")
    public List<PlaceResponse> places(@RequestParam(required = false) String search) {
        return masterDataService.listPlaces(search);
    }

    @PostMapping("/places")
    public PlaceResponse createPlace(@RequestBody PlaceRequest req) {
        return masterDataService.createPlace(req);
    }

    @PutMapping("/places/{id}")
    public PlaceResponse updatePlace(@PathVariable Long id, @RequestBody PlaceRequest req) {
        return masterDataService.updatePlace(id, req);
    }

    @DeleteMapping("/places/{id}")
    public void deletePlace(@PathVariable Long id) {
        masterDataService.deletePlace(id);
    }

    @GetMapping("/repledgers")
    public List<Repledger> repledgers(@RequestParam(required = false) String search) {
        return masterDataService.listRepledgers(search);
    }

    @PostMapping("/repledgers")
    public Repledger createRepledger(@RequestBody RepledgerRequest req) {
        return masterDataService.createRepledger(req);
    }

    @PutMapping("/repledgers/{id}")
    public Repledger updateRepledger(@PathVariable Long id, @RequestBody RepledgerRequest req) {
        return masterDataService.updateRepledger(id, req);
    }

    @DeleteMapping("/repledgers/{id}")
    public void deleteRepledger(@PathVariable Long id) {
        masterDataService.deleteRepledger(id);
    }

    @GetMapping("/lenders")
    public List<LenderResponse> lenders(@RequestParam(required = false) String search) {
        return masterDataService.listLenders(search);
    }

    @PostMapping("/lenders")
    public LenderResponse createLender(@RequestBody LenderRequest req) {
        return masterDataService.createLender(req);
    }

    @PutMapping("/lenders/{id}")
    public LenderResponse updateLender(@PathVariable Long id, @RequestBody LenderRequest req) {
        return masterDataService.updateLender(id, req);
    }

    @DeleteMapping("/lenders/{id}")
    public void deleteLender(@PathVariable Long id) {
        masterDataService.deleteLender(id);
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return masterDataService.getSettings();
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@RequestBody SettingsResponse req) {
        return masterDataService.updateSettings(req);
    }

    @PostMapping("/materials")
    public SettingsResponse addMaterial(@RequestBody java.util.Map<String, String> body) {
        return masterDataService.addMaterial(body.get("name"));
    }

    @DeleteMapping("/materials/{id}")
    public SettingsResponse deleteMaterial(@PathVariable Long id) {
        return masterDataService.deleteMaterial(id);
    }

    @GetMapping("/in-hand")
    public InHandSummary inHand() {
        return masterDataService.getInHandSummary();
    }

    @PostMapping("/in-hand")
    public InHandSummary addInHand(@RequestBody InHandRequest req) {
        return masterDataService.addInHandEntry(req);
    }

    @PostMapping("/in-hand/reset")
    public InHandSummary resetInHand() {
        return masterDataService.resetInHand();
    }

    @GetMapping("/repledges")
    public List<RepledgeListItem> repledges(@RequestParam(required = false) String search) {
        return repledgeService.listRepledges(search);
    }

    @GetMapping("/repledges/{loanId}")
    public RepledgeSummary vaultDetail(@PathVariable String loanId) {
        return repledgeService.getVaultDetail(loanId);
    }

    @PostMapping("/repledges/{loanId}/payments/principal")
    public RepledgeSummary payBankPrincipal(@PathVariable String loanId, @RequestBody PaymentRequest req) {
        return repledgeService.addBankPayment(loanId, TransactionType.principal, req);
    }

    @PostMapping("/repledges/{loanId}/payments/interest")
    public RepledgeSummary payBankInterest(@PathVariable String loanId, @RequestBody PaymentRequest req) {
        return repledgeService.addBankPayment(loanId, TransactionType.interest, req);
    }

    @GetMapping("/borrowings")
    public List<BorrowingResponse> borrowings(@RequestParam(required = false) String search) {
        return borrowingService.listBorrowings(search);
    }

    @GetMapping("/borrowings/{id}")
    public BorrowingResponse borrowing(@PathVariable Long id) {
        return borrowingService.getBorrowing(id);
    }

    @PostMapping("/borrowings")
    public BorrowingResponse createBorrowing(@RequestBody BorrowingRequest req) {
        return borrowingService.create(req);
    }

    @PostMapping("/borrowings/{id}/payments/principal")
    public BorrowingResponse payBorrowingPrincipal(@PathVariable Long id, @RequestBody PaymentRequest req) {
        return borrowingService.addPayment(id, TransactionType.principal, req);
    }

    @PostMapping("/borrowings/{id}/payments/interest")
    public BorrowingResponse payBorrowingInterest(@PathVariable Long id, @RequestBody PaymentRequest req) {
        return borrowingService.addPayment(id, TransactionType.interest, req);
    }

    @PostMapping("/borrowings/{id}/close")
    public BorrowingResponse closeBorrowing(@PathVariable Long id) {
        return borrowingService.close(id);
    }
}

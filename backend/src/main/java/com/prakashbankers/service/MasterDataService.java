package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import com.prakashbankers.util.FinancialMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MasterDataService {

    private final RepledgePlaceRepository placeRepository;
    private final RepledgerRepository repledgerRepository;
    private final LenderRepository lenderRepository;
    private final MaterialRepository materialRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final InHandEntryRepository inHandEntryRepository;
    private final LoanRepository loanRepository;
    private final BorrowingRepository borrowingRepository;
    private final InterestEngine interestEngine;

    public MasterDataService(RepledgePlaceRepository placeRepository, RepledgerRepository repledgerRepository,
                             LenderRepository lenderRepository, MaterialRepository materialRepository,
                             AppSettingsRepository appSettingsRepository, InHandEntryRepository inHandEntryRepository,
                             LoanRepository loanRepository, BorrowingRepository borrowingRepository,
                             InterestEngine interestEngine) {
        this.placeRepository = placeRepository;
        this.repledgerRepository = repledgerRepository;
        this.lenderRepository = lenderRepository;
        this.materialRepository = materialRepository;
        this.appSettingsRepository = appSettingsRepository;
        this.inHandEntryRepository = inHandEntryRepository;
        this.loanRepository = loanRepository;
        this.borrowingRepository = borrowingRepository;
        this.interestEngine = interestEngine;
    }

    // --- Places ---
    @Transactional(readOnly = true)
    public List<PlaceResponse> listPlaces(String search) {
        return placeRepository.findAll().stream()
                .filter(p -> search == null || search.isBlank()
                        || p.getName().toLowerCase().contains(search.toLowerCase())
                        || (p.getAddress() != null && p.getAddress().toLowerCase().contains(search.toLowerCase())))
                .map(this::toPlaceResponse)
                .toList();
    }

    @Transactional
    public PlaceResponse createPlace(PlaceRequest req) {
        RepledgePlace p = RepledgePlace.builder()
                .code(FinancialMapper.temporaryCode("PLC"))
                .name(req.getName())
                .type(parsePlaceType(req.getType()))
                .contact(req.getContact())
                .address(req.getAddress())
                .defaultRate(req.getDefaultRate())
                .build();
        placeRepository.saveAndFlush(p);
        p.setCode(FinancialMapper.orderedCode("PLC", p.getId()));
        return toPlaceResponse(placeRepository.save(p));
    }

    @Transactional
    public PlaceResponse updatePlace(Long id, PlaceRequest req) {
        RepledgePlace p = placeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setName(req.getName());
        p.setType(parsePlaceType(req.getType()));
        p.setContact(req.getContact());
        p.setAddress(req.getAddress());
        p.setDefaultRate(req.getDefaultRate());
        return toPlaceResponse(placeRepository.save(p));
    }

    private PlaceType parsePlaceType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place type is required");
        }
        for (PlaceType pt : PlaceType.values()) {
            if (pt.name().equalsIgnoreCase(type.trim())) {
                return pt;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid place type: " + type);
    }

    @Transactional
    public void deletePlace(Long id) {
        boolean inUse = loanRepository.findAllRepledgedWithDetails().stream()
                .anyMatch(l -> l.getRepledge().getPlace().getId().equals(id));
        if (inUse) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place is in use");
        placeRepository.deleteById(id);
    }

    // --- Repledgers ---
    @Transactional(readOnly = true)
    public List<Repledger> listRepledgers(String search) {
        return repledgerRepository.findAll().stream()
                .filter(r -> search == null || search.isBlank()
                        || r.getName().toLowerCase().contains(search.toLowerCase())
                        || r.getPhone().contains(search))
                .toList();
    }

    @Transactional
    public Repledger createRepledger(RepledgerRequest req) {
        Repledger repledger = Repledger.builder()
                .code(FinancialMapper.temporaryCode("RPL"))
                .name(req.getName()).role(req.getRole()).phone(req.getPhone()).build();
        repledgerRepository.saveAndFlush(repledger);
        repledger.setCode(FinancialMapper.orderedCode("RPL", repledger.getId()));
        return repledgerRepository.save(repledger);
    }

    @Transactional
    public Repledger updateRepledger(Long id, RepledgerRequest req) {
        Repledger r = repledgerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        r.setName(req.getName());
        r.setRole(req.getRole());
        r.setPhone(req.getPhone());
        return repledgerRepository.save(r);
    }

    @Transactional
    public void deleteRepledger(Long id) {
        boolean inUse = loanRepository.findAllRepledgedWithDetails().stream()
                .anyMatch(l -> l.getRepledge().getRepledger().getId().equals(id));
        if (inUse) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repledger is in use");
        repledgerRepository.deleteById(id);
    }

    // --- Lenders ---
    @Transactional(readOnly = true)
    public List<LenderResponse> listLenders(String search) {
        return lenderRepository.findAll().stream()
                .filter(l -> search == null || search.isBlank()
                        || l.getName().toLowerCase().contains(search.toLowerCase())
                        || l.getPhone().contains(search))
                .map(this::toLenderResponse)
                .toList();
    }

    @Transactional
    public LenderResponse createLender(LenderRequest req) {
        Lender lender = Lender.builder()
                .code(FinancialMapper.temporaryCode("LND"))
                .name(req.getName()).phone(req.getPhone()).address(req.getAddress())
                .defaultRate(req.getDefaultRate()).build();
        lenderRepository.saveAndFlush(lender);
        lender.setCode(FinancialMapper.orderedCode("LND", lender.getId()));
        return toLenderResponse(lenderRepository.save(lender));
    }

    @Transactional
    public LenderResponse updateLender(Long id, LenderRequest req) {
        Lender l = lenderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        l.setName(req.getName());
        l.setPhone(req.getPhone());
        l.setAddress(req.getAddress());
        l.setDefaultRate(req.getDefaultRate());
        return toLenderResponse(lenderRepository.save(l));
    }

    @Transactional
    public void deleteLender(Long id) {
        boolean inUse = borrowingRepository.findByLenderId(id).stream()
                .anyMatch(b -> b.getStatus() != BorrowingStatus.closed);
        if (inUse) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lender has active borrowing");
        lenderRepository.deleteById(id);
    }

    // --- Materials & Settings ---
    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        AppSettings s = getOrCreateSettings();
        return SettingsResponse.builder()
                .shopName(s.getShopName())
                .shopAddress(s.getShopAddress())
                .shopPhone(s.getShopPhone())
                .openingCapital(s.getOpeningCapital())
                .openingCapitalSet(s.isOpeningCapitalSet())
                .materials(materialRepository.findAllByOrderByNameAsc().stream().map(Material::getName).toList())
                .build();
    }

    @Transactional
    public SettingsResponse updateSettings(SettingsResponse req) {
        AppSettings s = getOrCreateSettings();
        if (req.getShopName() != null) s.setShopName(req.getShopName());
        if (req.getShopAddress() != null) s.setShopAddress(req.getShopAddress());
        if (req.getShopPhone() != null) s.setShopPhone(req.getShopPhone());
        if (req.getOpeningCapital() != null && !s.isOpeningCapitalSet()) {
            s.setOpeningCapital(req.getOpeningCapital());
            s.setOpeningCapitalSet(true);
            inHandEntryRepository.save(InHandEntry.builder()
                    .type(InHandType.opening)
                    .amount(req.getOpeningCapital())
                    .date(LocalDate.now())
                    .note("Opening business capital")
                    .build());
        }
        appSettingsRepository.save(s);
        return getSettings();
    }

    @Transactional
    public SettingsResponse addMaterial(String name) {
        if (materialRepository.findByNameIgnoreCase(name).isEmpty()) {
            materialRepository.save(Material.builder().name(name.trim()).build());
        }
        return getSettings();
    }

    @Transactional
    public SettingsResponse deleteMaterial(Long id) {
        materialRepository.deleteById(id);
        return getSettings();
    }

    @Transactional
    public InHandSummary resetInHand() {
        inHandEntryRepository.deleteAll();
        AppSettings s = getOrCreateSettings();
        s.setOpeningCapital(BigDecimal.ZERO);
        s.setOpeningCapitalSet(false);
        appSettingsRepository.save(s);
        return getInHandSummary();
    }

    // --- In Hand ---
    @Transactional(readOnly = true)
    public InHandSummary getInHandSummary() {
        AppSettings s = getOrCreateSettings();
        BigDecimal opening = s.getOpeningCapital() != null ? s.getOpeningCapital() : BigDecimal.ZERO;
        BigDecimal totalPut = sum(InHandType.put);
        BigDecimal totalTake = sum(InHandType.take);
        List<InHandResponse> entries = inHandEntryRepository.findAllByOrderByDateDescIdDesc().stream()
                .filter(e -> e.getType() != InHandType.opening)
                .map(this::toInHandResponse)
                .toList();
        return InHandSummary.builder()
                .openingCapital(opening)
                .totalPut(totalPut)
                .totalTake(totalTake)
                .balance(opening.add(totalPut).subtract(totalTake))
                .entries(entries)
                .build();
    }

    @Transactional
    public InHandSummary addInHandEntry(InHandRequest req) {
        InHandType type = InHandType.valueOf(req.getType().toLowerCase());
        if (type == InHandType.opening) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use settings to set opening capital");
        }
        inHandEntryRepository.save(InHandEntry.builder()
                .type(type)
                .amount(req.getAmount())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .note(req.getNote())
                .build());
        return getInHandSummary();
    }

    private BigDecimal sum(InHandType type) {
        return inHandEntryRepository.findByType(type).stream()
                .map(InHandEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PlaceResponse toPlaceResponse(RepledgePlace p) {
        List<Loan> atPlace = loanRepository.findAllRepledgedWithDetails().stream()
                .filter(l -> l.getRepledge().getPlace().getId().equals(p.getId()))
                .toList();
        BigDecimal liability = BigDecimal.ZERO;
        for (Loan l : atPlace) {
            FinancialSummaryDto fin = interestEngine.calculate(
                    FinancialMapper.repledgeInput(l.getRepledge(), l, LocalDate.now()));
            liability = liability.add(fin.getCurrentPrincipal());
        }
        return PlaceResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType().name())
                .contact(p.getContact())
                .address(p.getAddress())
                .defaultRate(p.getDefaultRate())
                .activeItems(atPlace.size())
                .totalLiability(liability)
                .build();
    }

    private InHandResponse toInHandResponse(InHandEntry e) {
        return InHandResponse.builder()
                .id(e.getId())
                .type(e.getType().name())
                .amount(e.getAmount())
                .date(e.getDate())
                .note(e.getNote())
                .build();
    }

    private AppSettings getOrCreateSettings() {
        return appSettingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> appSettingsRepository.save(AppSettings.builder().build()));
    }

    private LenderResponse toLenderResponse(Lender l) {
        return LenderResponse.builder()
                .id(l.getId())
                .code(l.getCode())
                .name(l.getName())
                .phone(l.getPhone())
                .address(l.getAddress())
                .defaultRate(l.getDefaultRate())
                .build();
    }
}

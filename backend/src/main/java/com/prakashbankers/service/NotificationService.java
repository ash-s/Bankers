package com.prakashbankers.service;

import com.prakashbankers.dto.ApiDtos.*;
import com.prakashbankers.dto.FinancialSummaryDto;
import com.prakashbankers.entity.*;
import com.prakashbankers.repository.*;
import com.prakashbankers.util.FinancialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationSettingsRepository settingsRepository;
    private final AppNotificationRepository notificationRepository;
    private final LoanRepository loanRepository;
    private final InterestEngine interestEngine;

    public NotificationService(NotificationSettingsRepository settingsRepository,
                               AppNotificationRepository notificationRepository,
                               LoanRepository loanRepository,
                               InterestEngine interestEngine) {
        this.settingsRepository = settingsRepository;
        this.notificationRepository = notificationRepository;
        this.loanRepository = loanRepository;
        this.interestEngine = interestEngine;
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings() {
        NotificationSettings s = getOrCreate();
        return toSettingsResponse(s);
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(NotificationSettingsRequest req) {
        NotificationSettings s = getOrCreate();
        if (req.getDuePreset() != null) s.setDuePreset(req.getDuePreset());
        if (req.getCustomDueDays() != null) s.setCustomDueDays(req.getCustomDueDays());
        if (req.getPushEnabled() != null) s.setPushEnabled(req.getPushEnabled());
        if (req.getSmsEnabled() != null) s.setSmsEnabled(req.getSmsEnabled());
        if (req.getWhatsappEnabled() != null) s.setWhatsappEnabled(req.getWhatsappEnabled());
        if (req.getSmsTemplate() != null) s.setSmsTemplate(req.getSmsTemplate());
        if (req.getWhatsappTemplate() != null) s.setWhatsappTemplate(req.getWhatsappTemplate());
        return toSettingsResponse(settingsRepository.save(s));
    }

    /** Sync overdue notifications once per app session start */
    @Transactional
    public void syncOnStartup() {
        NotificationSettings settings = getOrCreate();
        if (!settings.isPushEnabled()) return;

        int dueDays = resolveDueDays(settings);
        LocalDate today = LocalDate.now();

        loanRepository.findAllWithDetails().stream()
                .filter(l -> l.getStatus() != LoanStatus.completed)
                // Only notify once the loan has reached the configured "interest due"
                // age (30d / 180d / 365d / custom) - younger loans are not due yet.
                .filter(l -> l.getPledgeDate() != null
                        && ChronoUnit.DAYS.between(l.getPledgeDate(), today) >= dueDays)
                .forEach(l -> {
                    FinancialSummaryDto fin = interestEngine.calculate(FinancialMapper.loanInput(l, today));
                    if (fin.getOverdueCycles() < 1) return;
                    if (notificationRepository.findByLoanId(l.getLoanId()).isPresent()) return;

                    notificationRepository.save(AppNotification.builder()
                            .loanId(l.getLoanId())
                            .customerId(l.getCustomer().getId())
                            .customerName(l.getCustomer().getName())
                            .customerPhone(l.getCustomer().getPhone())
                            .item(l.getItem())
                            .overdueCycles(fin.getOverdueCycles())
                            .interestBalance(fin.getInterestBalance())
                            .readFlag(false)
                            .build());

                    if (settings.isSmsEnabled()) {
                        sendSms(l, settings, fin);
                    }
                    if (settings.isWhatsappEnabled()) {
                        sendWhatsapp(l, settings, fin);
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<AppNotificationResponse> listNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByReadFlagFalse();
    }

    @Transactional
    public void markRead(Long id) {
        AppNotification n = notificationRepository.findById(id)
                .orElseThrow();
        n.setReadFlag(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.findAll().forEach(n -> n.setReadFlag(true));
    }

    /** Grace/term setting: "Notify when interest is due after ..." (see Settings UI). */
    private int resolveDueDays(NotificationSettings s) {
        return switch (s.getDuePreset() != null ? s.getDuePreset() : "30d") {
            case "180d", "6m" -> 180;
            case "365d", "1y" -> 365;
            case "custom" -> s.getCustomDueDays() != null ? s.getCustomDueDays() : 30;
            default -> 30;
        };
    }

    private void sendSms(Loan l, NotificationSettings s, FinancialSummaryDto fin) {
        String template = s.getSmsTemplate() != null ? s.getSmsTemplate()
                : "Dear {name}, interest of Rs.{amount} is due on {item}. Please visit Prakash Bankers.";
        String msg = template
                .replace("{name}", l.getCustomer().getName())
                .replace("{amount}", fin.getInterestBalance().toPlainString())
                .replace("{item}", l.getItem())
                .replace("{phone}", l.getCustomer().getPhone() != null ? l.getCustomer().getPhone() : "");
        System.out.println("[SMS] " + l.getCustomer().getPhone() + ": " + msg);
    }

    private void sendWhatsapp(Loan l, NotificationSettings s, FinancialSummaryDto fin) {
        String template = s.getWhatsappTemplate() != null ? s.getWhatsappTemplate()
                : "Hi {name}, your pledge *{item}* has interest due: Rs.{amount}. Contact us at Prakash Bankers.";
        String msg = template
                .replace("{name}", l.getCustomer().getName())
                .replace("{amount}", fin.getInterestBalance().toPlainString())
                .replace("{item}", l.getItem());
        System.out.println("[WhatsApp] " + l.getCustomer().getPhone() + ": " + msg);
    }

    private NotificationSettings getOrCreate() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(NotificationSettings.builder().build()));
    }

    private NotificationSettingsResponse toSettingsResponse(NotificationSettings s) {
        return NotificationSettingsResponse.builder()
                .duePreset(s.getDuePreset())
                .customDueDays(s.getCustomDueDays())
                .pushEnabled(s.isPushEnabled())
                .smsEnabled(s.isSmsEnabled())
                .whatsappEnabled(s.isWhatsappEnabled())
                .smsTemplate(s.getSmsTemplate())
                .whatsappTemplate(s.getWhatsappTemplate())
                .build();
    }

    private AppNotificationResponse toResponse(AppNotification n) {
        return AppNotificationResponse.builder()
                .id(n.getId())
                .loanId(n.getLoanId())
                .customerId(n.getCustomerId())
                .customerName(n.getCustomerName())
                .customerPhone(n.getCustomerPhone())
                .item(n.getItem())
                .overdueCycles(n.getOverdueCycles())
                .interestBalance(n.getInterestBalance())
                .read(n.isReadFlag())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

package pl.kurs.sogaapplication.service.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SuspiciousBill;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;
import pl.kurs.sogaapplication.service.core.RachunekService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Serwis walidacji podejrzanych rachunków
 * Wykrywa potencjalnie problematyczne transakcje
 */
@Service
public class BillValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillValidationService.class);
    
    private final RachunekService rachunekService;
    private final RestaurantConfigService configService;
    
    // Konfiguracja walidacji
    @Value("${restaurant.validation.suspicious.amount:1000}")
    private BigDecimal suspiciousAmount;
    
    @Value("${restaurant.validation.suspicious.duration:10}")
    private int suspiciousDurationMinutes;
    
    @Value("${restaurant.validation.very-suspicious.amount:2000}")
    private BigDecimal verySuspiciousAmount;
    
    @Value("${restaurant.validation.very-suspicious.duration:5}")
    private int verySuspiciousDurationMinutes;
    
    @Value("${restaurant.validation.enabled:true}")
    private boolean validationEnabled;
    
    public BillValidationService(RachunekService rachunekService, RestaurantConfigService configService) {
        this.rachunekService = rachunekService;
        this.configService = configService;
    }
    
    /**
     * Znajduje podejrzane rachunki dla danego okresu
     */
    public List<SuspiciousBill> findSuspiciousBills(LocalDate from, LocalDate to) {
        if (!validationEnabled) {
            logger.debug("Walidacja rachunków jest wyłączona");
            return List.of();
        }
        
        logger.info("Szukanie podejrzanych rachunków od {} do {}", from, to);
        
        List<Rachunek> bills = rachunekService.findByDateRange(from, to);
        List<SuspiciousBill> suspiciousBills = new ArrayList<>();
        
        for (Rachunek bill : bills) {
            // Sprawdź wszystkie kryteria podejrzanych rachunków
            List<SuspiciousBill> billIssues = validateBill(bill);
            suspiciousBills.addAll(billIssues);
        }
        
        logger.info("Znaleziono {} podejrzanych rachunków", suspiciousBills.size());
        return suspiciousBills;
    }
    
    /**
     * Waliduje pojedynczy rachunek - wszystkie warunki muszą być spełnione jednocześnie
     */
    public List<SuspiciousBill> validateBill(Rachunek bill) {
        List<SuspiciousBill> issues = new ArrayList<>();
        
        // Sprawdź czy sprzedawca jest w liście wszystkich sprzedawców
        Integer sellerId = bill.getUzytkownik() != null ? bill.getUzytkownik().getId() : null;
        if (sellerId == null || !configService.getAllSellers().contains(sellerId)) {
            return issues; // Nie waliduj rachunków spoza listy sprzedawców
        }
        
        // GŁÓWNA WALIDACJA: Wszystkie warunki muszą być spełnione jednocześnie
        // 1. Kwota > suspiciousAmount (1000 zł)
        // 2. Czas < suspiciousDurationMinutes (10 minut)  
        // 3. Sprzedawca w AllSellerIds (już sprawdzone wyżej)
        if (isHighAmountShortDuration(bill)) {
            // DODATKOWA REGUŁA: jeśli rachunek to głównie abonament / duże pakiety,
            // nie traktuj go jako podejrzany.
            if (isHighSubscriptionBill(bill)) {
                logger.debug("Rachunek {}: wysoka kwota + krótki czas, ale zawiera abonament/kupony - pomijam z podejrzanych", bill.getId());
                return issues;
            }

            String reason = String.format("PODEJRZANY RACHUNEK: %.2f zł w czasie %s (sprzedawca: %s)", 
                    bill.getWartNu(), getDurationFormatted(bill), getSellerName(bill));
            String severity = isVerySuspicious(bill) ? "BARDZO PODEJRZANY" : "PODEJRZANY";
            issues.add(SuspiciousBill.from(bill, reason, severity));
        }
        
        // Dodatkowe walidacje (opcjonalne - można wyłączyć w konfiguracji)
        
        // Walidacja 2: Rachunek w przyszłości (błąd daty)
        if (bill.getDataRozpoczecia().isAfter(LocalDateTime.now())) {
            String reason = "Rachunek w przyszłości - błąd daty";
            String severity = "BŁĄD DATY";
            issues.add(SuspiciousBill.from(bill, reason, severity));
        }
        
        return issues;
    }
    
    /**
     * Sprawdza czy rachunek spełnia kryteria wysokiej kwoty i krótkiego czasu
     */
    private boolean isHighAmountShortDuration(Rachunek bill) {
        return bill.getWartNu().compareTo(suspiciousAmount) > 0 &&
               getDuration(bill).toMinutes() < suspiciousDurationMinutes;
    }
    
    /**
     * Sprawdza czy rachunek jest bardzo podejrzany
     */
    private boolean isVerySuspicious(Rachunek bill) {
        return bill.getWartNu().compareTo(verySuspiciousAmount) > 0 &&
               getDuration(bill).toMinutes() < verySuspiciousDurationMinutes;
    }
    
    /**
     * Zwraca czas trwania rachunku
     */
    private Duration getDuration(Rachunek bill) {
        return Duration.between(bill.getDataRozpoczecia(), bill.getDataZakonczenia());
    }
    
    /**
     * Zwraca sformatowany czas trwania
     */
    private String getDurationFormatted(Rachunek bill) {
        Duration duration = getDuration(bill);
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "min";
        }
    }

    /**
     * Rachunek o wysokiej kwocie, ale zawierający "abonamentowe" towary (np. miesięczne abonamenty),
     * nie powinien być traktowany jako podejrzany.
     *
     * Aktualnie traktujemy jako abonamentowe towary o ID_TW: 4794, 4468.
     * Wystarczy, że na rachunku pojawi się chociaż jedna taka pozycja.
     */
    private boolean isHighSubscriptionBill(Rachunek bill) {
        // ID_TW produktów abonamentowych / kuponów
        Set<Long> subscriptionProductIds = Set.of(4794L, 4468L);

        if (bill.getPozycje() == null || bill.getPozycje().isEmpty()) {
            return false;
        }

        return bill.getPozycje().stream()
                .filter(p -> p.getTowar() != null && p.getTowar().getIdTowaru() != null)
                .anyMatch(p -> subscriptionProductIds.contains(p.getTowar().getIdTowaru()));
    }
    
    /**
     * Zwraca nazwę sprzedawcy
     */
    private String getSellerName(Rachunek bill) {
        if (bill.getUzytkownik() == null) {
            return "Nieznany";
        }
        
        if (org.hibernate.Hibernate.isInitialized(bill.getUzytkownik())) {
            return bill.getUzytkownik().getNazwaUzytkownika();
        } else {
            return "ID: " + bill.getUzytkownik().getId();
        }
    }
    
    /**
     * Zwraca statystyki podejrzanych rachunków
     */
    public SuspiciousBillStats getStats(List<SuspiciousBill> suspiciousBills) {
        if (suspiciousBills.isEmpty()) {
            return new SuspiciousBillStats(0, 0, BigDecimal.ZERO, 0, 0);
        }
        
        long totalCount = suspiciousBills.size();
        long verySuspiciousCount = suspiciousBills.stream()
                .mapToLong(bill -> bill.severity().equals("BARDZO PODEJRZANY") ? 1 : 0)
                .sum();
        
        BigDecimal totalAmount = suspiciousBills.stream()
                .map(SuspiciousBill::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long highAmountCount = suspiciousBills.stream()
                .mapToLong(bill -> bill.amount().compareTo(verySuspiciousAmount) > 0 ? 1 : 0)
                .sum();
        
        long shortDurationCount = suspiciousBills.stream()
                .mapToLong(bill -> bill.duration().toMinutes() < verySuspiciousDurationMinutes ? 1 : 0)
                .sum();
        
        return new SuspiciousBillStats(
                totalCount, verySuspiciousCount, totalAmount, highAmountCount, shortDurationCount
        );
    }
    
    /**
     * Statystyki podejrzanych rachunków
     */
    public record SuspiciousBillStats(
            long totalCount,
            long verySuspiciousCount,
            BigDecimal totalAmount,
            long highAmountCount,
            long shortDurationCount
    ) {}
}

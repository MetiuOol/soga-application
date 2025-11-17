package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.PodsumowanieTygodnia;
import pl.kurs.sogaapplication.models.ObrotDzien;
import pl.kurs.sogaapplication.models.ObrotDzienGodzinaView;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Serwis do analizy czasowej sprzedaży
 * Odpowiada za analizy godzinowe, dzienne, tygodniowe i roczne
 */
@Service
public class TimeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeAnalysisService.class);
    
    private final RachunekJpaRepository rachunekRepository;
    
    public TimeAnalysisService(RachunekJpaRepository rachunekRepository) {
        this.rachunekRepository = rachunekRepository;
    }
    
    /**
     * Analizuje sprzedaż po godzinach dla konkretnego dnia
     */
    @Transactional(readOnly = true)
    public List<ObrotSprzedawcyGodzina> analyzeHourlySales(LocalDate date) {
        logger.info("Analiza sprzedaży po godzinach dla dnia: {}", date);
        
        var hourlySales = rachunekRepository.sumyGodzinowe(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        
        return hourlySales.stream()
                .map(r -> new ObrotSprzedawcyGodzina(
                        r.sellerId(),
                        r.sellerName(),
                        r.godzina(),
                        r.suma()))
                .toList();
    }
    
    /**
     * Analizuje sprzedaż po godzinach dla zakresu dni i sprzedawców
     */
    @Transactional(readOnly = true)
    public List<ObrotDzienGodzinaView> analyzeHourlySalesByDayAndSeller(LocalDate from, LocalDate to, 
                                                                        Collection<Integer> sellerIds) {
        logger.info("Analiza sprzedaży po godzinach od {} do {} dla sprzedawców: {}", 
                from, to, sellerIds);
        
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
        
        return rachunekRepository.sumyDzienGodzina(fromDateTime, toDateTime, sellerIds);
    }
    
    /**
     * Analizuje sprzedaż dzienną dla zakresu dni
     */
    @Transactional(readOnly = true)
    public List<ObrotDzien> analyzeDailySales(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        logger.info("Analiza sprzedaży dziennej od {} do {} dla sprzedawców: {}", 
                from, to, sellerIds);
        
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
        
        return rachunekRepository.sumyDzien(fromDateTime, toDateTime, sellerIds);
    }
    
    /**
     * Generuje podsumowanie roczne
     */
    @Transactional(readOnly = true)
    public List<PodsumowanieTygodnia> generateYearlySummary(int year) {
        logger.info("Generowanie podsumowania rocznego dla roku: {}", year);
        
        var yearlyReport = generateYearlyReport(year);
        yearlyReport.forEach(report -> logger.info("Tydzień {}: {} rachunków, {} osób, sprzedaż: {}", 
                report.getNumerTygodnia(), report.getLiczbaRachunkow(), 
                report.getLacznaIloscOsob(), report.getSumaNetto()));
        
        return yearlyReport;
    }
    
    /**
     * Generuje podsumowanie roczne (prywatna metoda z logiką)
     */
    private List<PodsumowanieTygodnia> generateYearlyReport(int year) {
        var podsumowania = new java.util.ArrayList<PodsumowanieTygodnia>();
        
        LocalDate start = LocalDate.of(year, 1, 1);
        int numerTygodnia = 1;
        
        while (start.getYear() == year) {
            LocalDate endDate = start.plusDays(6); // 7 dniowy zakres
            if (endDate.getYear() > year) {
                endDate = LocalDate.of(year, 12, 31); // ostatni dzień roku
            }
            
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            var rachunki = rachunekRepository.findByDataRozpoczeciaBetween(startDateTime, endDateTime);
            
            var sumaNetto = rachunki.stream()
                    .map(pl.kurs.sogaapplication.models.Rachunek::getWartNu)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            var sumaBrutto = rachunki.stream()
                    .map(pl.kurs.sogaapplication.models.Rachunek::getWartBu)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            int liczbaOsob = rachunki.stream()
                    .mapToInt(pl.kurs.sogaapplication.models.Rachunek::getIloscOsob)
                    .sum();
            
            podsumowania.add(new PodsumowanieTygodnia(
                    year,
                    numerTygodnia,
                    sumaNetto,
                    sumaBrutto,
                    rachunki.size(),
                    liczbaOsob
            ));
            
            // przejście do kolejnego tygodnia
            start = start.plusDays(7);
            numerTygodnia++;
        }
        
        return podsumowania;
    }
    
    /**
     * Wyświetla analizę godzinową w czytelnym formacie
     */
    public void printHourlyAnalysis(List<ObrotSprzedawcyGodzina> data) {
        var kwota = java.text.NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        kwota.setMinimumFractionDigits(2);
        kwota.setMaximumFractionDigits(2);
        
        Map<pl.kurs.sogaapplication.models.SellerKey, List<ObrotSprzedawcyGodzina>> bySeller =
                data.stream()
                        .collect(Collectors.groupingBy(
                                d -> new pl.kurs.sogaapplication.models.SellerKey(d.sellerId(), d.sellerName()),
                                Collectors.toList()
                        ));
        
        bySeller.forEach((seller, list) -> {
            logger.info("Sprzedawca: {} ({})", seller.nazwa(), seller.id());
            
            list.stream()
                    .sorted(Comparator.comparingInt(d -> d.godzina().intValue()))
                    .forEach(d -> logger.info("  {}:00  —  {}", 
                            String.format("%02d", d.godzina()), kwota.format(d.suma())));
            
            var sumaDlaSprzedawcy = list.stream()
                    .map(ObrotSprzedawcyGodzina::suma)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            logger.info("  Razem: {}", kwota.format(sumaDlaSprzedawcy));
        });
    }
    
    /**
     * Wyświetla analizę dzienną w czytelnym formacie
     */
    public void printDailyAnalysis(List<ObrotDzien> results) {
        String[] dni = {"Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sb"};
        var kwota = java.text.NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        kwota.setMinimumFractionDigits(2);
        kwota.setMaximumFractionDigits(2);
        
        // grupowanie: dzień -> godzina -> suma
        Map<Integer, Map<Integer, java.math.BigDecimal>> sumaPerDzienGodzina = results.stream()
                .collect(Collectors.groupingBy(
                        ObrotDzien::getDzien, TreeMap::new,
                        Collectors.groupingBy(
                                ObrotDzien::getGodzina, TreeMap::new,
                                Collectors.reducing(java.math.BigDecimal.ZERO, ObrotDzien::getSuma, java.math.BigDecimal::add)
                        )
                ));
        
        // drukowanie
        sumaPerDzienGodzina.forEach((dzien, mapaGodzin) -> {
            logger.info("{}:", dni[dzien]);
            
            mapaGodzin.forEach((godzina, suma) ->
                    logger.info("  {}:00 — {}", String.format("%02d", godzina), kwota.format(suma)));
            
            // suma dnia
            java.math.BigDecimal sumaDnia = mapaGodzin.values().stream()
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            logger.info("  Razem: {}", kwota.format(sumaDnia));
        });
        
        // suma całkowita
        java.math.BigDecimal sumaRazem = sumaPerDzienGodzina.values().stream()
                .flatMap(m -> m.values().stream())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        logger.info("Razem wszystkie dni: {}", kwota.format(sumaRazem));
    }
}








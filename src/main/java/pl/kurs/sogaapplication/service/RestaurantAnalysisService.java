package pl.kurs.sogaapplication.service;

import org.springframework.stereotype.Service;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.service.analysis.SalesAnalysisService;
import pl.kurs.sogaapplication.service.analysis.TimeAnalysisService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;
import pl.kurs.sogaapplication.service.export.ReportExportService;
import pl.kurs.sogaapplication.service.validation.BillValidationService;

import java.time.LocalDate;
import java.util.List;

/**
 * Główny serwis do analizy sprzedaży w restauracji
 * Orkiestruje wszystkie inne serwisy i dostarcza wysokopoziomowe API
 */
@Service
public class RestaurantAnalysisService {
    
    
    private final SalesAnalysisService salesAnalysisService;
    private final TimeAnalysisService timeAnalysisService;
    private final ReportExportService reportExportService;
    private final RestaurantConfigService configService;
    private final BillValidationService billValidationService;
    
    public RestaurantAnalysisService(SalesAnalysisService salesAnalysisService,
                                   TimeAnalysisService timeAnalysisService,
                                   ReportExportService reportExportService,
                                   RestaurantConfigService configService,
                                   BillValidationService billValidationService) {
        this.salesAnalysisService = salesAnalysisService;
        this.timeAnalysisService = timeAnalysisService;
        this.reportExportService = reportExportService;
        this.configService = configService;
        this.billValidationService = billValidationService;
    }
    
    /**
     * Generuje pełny raport restauracji dla danego okresu
     */
    public RestaurantReportDto generateRestaurantReport(LocalDate from, LocalDate to) {
        return salesAnalysisService.generateSalesReport(from, to);
    }
    
    /**
     * Generuje pełny raport restauracji dla danego okresu i sprzedawców
     */
    public RestaurantReportDto generateRestaurantReport(LocalDate from, LocalDate to, List<Integer> sellerIds) {
        return salesAnalysisService.generateSalesReport(from, to, sellerIds);
    }
    
    /**
     * Analiza sprzedaży po godzinach dla konkretnego dnia
     */
    public void analyzeHourlySales(LocalDate date) {
        var hourlySales = timeAnalysisService.analyzeHourlySales(date);
        timeAnalysisService.printHourlyAnalysis(hourlySales);
    }
    
    /**
     * Analiza sprzedaży po godzinach dla zakresu dni
     */
    public void analyzeHourlySales(LocalDate from, LocalDate to, List<Integer> sellerIds) {
        timeAnalysisService.analyzeHourlySalesByDayAndSeller(from, to, sellerIds);
        // Tu można dodać dodatkowe formatowanie jeśli potrzeba
    }
    
    /**
     * Podsumowanie roczne
     */
    public void generateYearlySummary(int year) {
        timeAnalysisService.generateYearlySummary(year);
    }
    
    /**
     * Eksportuje raport do XML
     */
    public void exportReportToXml(LocalDate from, LocalDate to, List<Integer> sellerIds) {
        var report = generateRestaurantReport(from, to, sellerIds);
        reportExportService.exportToXml(report);
    }
    
    /**
     * Eksportuje raport do CSV
     */
    public void exportReportToCsv(LocalDate from, LocalDate to, List<Integer> sellerIds) {
        var report = generateRestaurantReport(from, to, sellerIds);
        reportExportService.exportToCsv(report);
    }
    
    // Metody publiczne do dostępu do konfiguracji
    
    public List<Integer> getKitchenGroupIds() {
        return configService.getKitchenGroups();
    }
    
    public List<Integer> getDefaultSellerIds() {
        return configService.getDefaultSellers();
    }
    
    public List<Integer> getAllSellerIds() {
        return configService.getAllSellers();
    }
    
    /**
     * Waliduje podejrzane rachunki dla danego okresu
     */
    public List<pl.kurs.sogaapplication.models.SuspiciousBill> validateSuspiciousBills(LocalDate from, LocalDate to) {
        return billValidationService.findSuspiciousBills(from, to);
    }
    
    /**
     * Zwraca statystyki podejrzanych rachunków
     */
    public BillValidationService.SuspiciousBillStats getSuspiciousBillStats(List<pl.kurs.sogaapplication.models.SuspiciousBill> suspiciousBills) {
        return billValidationService.getStats(suspiciousBills);
    }
}

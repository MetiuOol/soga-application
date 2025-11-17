package pl.kurs.sogaapplication.examples;

import org.springframework.stereotype.Component;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.service.analysis.SalesAnalysisService;
import pl.kurs.sogaapplication.service.analysis.TimeAnalysisService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;
import pl.kurs.sogaapplication.service.display.ReportFormatter;
import pl.kurs.sogaapplication.service.export.ReportExportService;

import java.time.LocalDate;
import java.util.List;

/**
 * Przykłady użycia systemu analizy restauracji
 * Pokazuje różne sposoby korzystania z nowej architektury
 */
@Component
public class UsageExamples {
    
    private final SalesAnalysisService salesAnalysisService;
    private final TimeAnalysisService timeAnalysisService;
    private final ReportExportService reportExportService;
    private final RestaurantConfigService configService;
    private final ReportFormatter formatter;
    
    public UsageExamples(SalesAnalysisService salesAnalysisService,
                        TimeAnalysisService timeAnalysisService,
                        ReportExportService reportExportService,
                        RestaurantConfigService configService,
                        ReportFormatter formatter) {
        this.salesAnalysisService = salesAnalysisService;
        this.timeAnalysisService = timeAnalysisService;
        this.reportExportService = reportExportService;
        this.configService = configService;
        this.formatter = formatter;
    }
    
    /**
     * Przykład 1: Podstawowy raport sprzedaży
     */
    public void example1_BasicSalesReport() {
        System.out.println("=== PRZYKŁAD 1: Podstawowy raport sprzedaży ===");
        
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);
        
        // Generuj raport dla domyślnych sprzedawców
        RestaurantReportDto report = salesAnalysisService.generateSalesReport(from, to);
        
        // Wyświetl sformatowany raport
        System.out.println(formatter.formatRestaurantReport(report));
    }
    
    /**
     * Przykład 2: Raport dla konkretnych sprzedawców
     */
    public void example2_CustomSellersReport() {
        System.out.println("=== PRZYKŁAD 2: Raport dla konkretnych sprzedawców ===");
        
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);
        List<Integer> customSellers = List.of(11, 12, 13); // Tylko wybrani sprzedawcy
        
        RestaurantReportDto report = salesAnalysisService.generateSalesReport(from, to, customSellers);
        
        System.out.println("Raport dla sprzedawców: " + customSellers);
        System.out.println(formatter.formatRestaurantReport(report));
    }
    
    /**
     * Przykład 3: Analiza godzinowa
     */
    public void example3_HourlyAnalysis() {
        System.out.println("=== PRZYKŁAD 3: Analiza godzinowa ===");
        
        LocalDate date = LocalDate.of(2025, 7, 15);
        
        List<ObrotSprzedawcyGodzina> hourlyData = timeAnalysisService.analyzeHourlySales(date);
        
        System.out.println("Analiza godzinowa dla dnia: " + date);
        System.out.println(formatter.formatHourlyAnalysis(hourlyData));
    }
    
    /**
     * Przykład 4: Podsumowanie roczne
     */
    public void example4_YearlySummary() {
        System.out.println("=== PRZYKŁAD 4: Podsumowanie roczne ===");
        
        int year = 2024;
        var yearlyData = timeAnalysisService.generateYearlySummary(year);
        
        System.out.println("Podsumowanie roku: " + year);
        System.out.println(formatter.formatYearlySummary(yearlyData));
    }
    
    /**
     * Przykład 5: Eksport raportów
     */
    public void example5_ExportReports() {
        System.out.println("=== PRZYKŁAD 5: Eksport raportów ===");
        
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);
        List<Integer> sellerIds = configService.getDefaultSellers();
        
        // Generuj raport
        RestaurantReportDto report = salesAnalysisService.generateSalesReport(from, to, sellerIds);
        
        // Eksportuj do różnych formatów
        reportExportService.exportToXml(report);
        reportExportService.exportToCsv(report);
        
        System.out.println("✅ Raporty zostały wyeksportowane do XML i CSV");
    }
    
    /**
     * Przykład 6: Analiza dzienna dla miesiąca
     */
    public void example6_DailyAnalysis() {
        System.out.println("=== PRZYKŁAD 6: Analiza dzienna dla miesiąca ===");
        
        LocalDate firstDayOfMonth = LocalDate.of(2025, 7, 1);
        List<Integer> sellerIds = configService.getDefaultSellers();
        
        var dailySales = salesAnalysisService.analyzeDailySales(firstDayOfMonth, sellerIds);
        
        System.out.println("Analiza dzienna dla miesiąca: " + firstDayOfMonth.getMonth());
        dailySales.forEach(day -> {
            System.out.printf("%s: Kuchnia=%s, Bufet=%s, Razem=%s%n",
                    day.dzien(),
                    day.kuchnia(),
                    day.bufet(),
                    day.suma());
        });
    }
    
    /**
     * Przykład 7: Analiza kuchnia vs bufet
     */
    public void example7_KitchenBuffetAnalysis() {
        System.out.println("=== PRZYKŁAD 7: Analiza kuchnia vs bufet ===");
        
        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);

        var kitchenBuffetAnalysis = salesAnalysisService.analyzeKitchenBuffetSales(
                from.atStartOfDay(),
                to.atTime(23, 59, 59),
                configService.getKitchenProducts(),
                configService.getPackagingProducts(),
                configService.getDeliveryProducts());

        System.out.println("Analiza kuchnia / bufet / opakowania / dowóz (po konkretnych towarach - ID_TW):");
        System.out.println("🍳 Kuchnia:      " + kitchenBuffetAnalysis.kuchniaNetto());
        System.out.println("🥤 Bufet:        " + kitchenBuffetAnalysis.bufetNetto());
        System.out.println("📦 Opakowania:   " + kitchenBuffetAnalysis.opakowaniaNetto());
        System.out.println("🚚 Dowóz:        " + kitchenBuffetAnalysis.dowozNetto());
        System.out.println("📊 Razem:        " + kitchenBuffetAnalysis.sumaRazem());
    }
    
    /**
     * Przykład 8: Konfiguracja restauracji
     */
    public void example8_RestaurantConfiguration() {
        System.out.println("=== PRZYKŁAD 8: Konfiguracja restauracji ===");
        
        System.out.println("🏠 Grupy towarów kuchennych:");
        System.out.println("   " + configService.getKitchenGroups());
        
        System.out.println("\n👥 Domyślni sprzedawcy:");
        System.out.println("   " + configService.getDefaultSellers());
        
        System.out.println("\n👥 Wszyscy sprzedawcy:");
        System.out.println("   " + configService.getAllSellers());
        
        // Przykład sprawdzania konfiguracji
        System.out.println("\n🔍 Przykłady sprawdzania:");
        System.out.println("Grupa 39 to kuchnia? " + configService.isKitchenGroup(39));
        System.out.println("Sprzedawca 11 to domyślny? " + configService.isDefaultSeller(11));
    }
    
    /**
     * Uruchamia wszystkie przykłady
     */
    public void runAllExamples() {
        System.out.println("🚀 URUCHAMIANIE WSZYSTKICH PRZYKŁADÓW");
        System.out.println("=" .repeat(60));
        
        try {
            example1_BasicSalesReport();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example2_CustomSellersReport();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example3_HourlyAnalysis();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example4_YearlySummary();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example5_ExportReports();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example6_DailyAnalysis();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example7_KitchenBuffetAnalysis();
            System.out.println("\n" + "=".repeat(60) + "\n");
            
            example8_RestaurantConfiguration();
            
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas uruchamiania przykładów: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n✅ Wszystkie przykłady zostały uruchomione!");
    }
}






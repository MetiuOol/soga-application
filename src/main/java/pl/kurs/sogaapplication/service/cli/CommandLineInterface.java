package pl.kurs.sogaapplication.service.cli;

import org.springframework.stereotype.Component;
import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.business.PointOfSale;
import pl.kurs.sogaapplication.service.analysis.FoodCostService;
import pl.kurs.sogaapplication.service.analysis.SalesAnalysisService;
import pl.kurs.sogaapplication.service.analysis.TimeAnalysisService;
import pl.kurs.sogaapplication.service.config.PointOfSaleService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;
import pl.kurs.sogaapplication.service.display.ReportFormatter;
import pl.kurs.sogaapplication.service.export.ReportExportService;
import pl.kurs.sogaapplication.service.validation.BillValidationService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Interfejs linii komend dla aplikacji restauracyjnej
 */
@Component
public class CommandLineInterface {
    
    private final SalesAnalysisService salesAnalysisService;
    private final TimeAnalysisService timeAnalysisService;
    private final ReportExportService reportExportService;
    private final RestaurantConfigService configService;
    private final PointOfSaleService pointOfSaleService;
    private final ReportFormatter formatter;
    private final BillValidationService billValidationService;
    private final FoodCostService foodCostService;
    
    private final Scanner scanner = new Scanner(System.in);
    
    public CommandLineInterface(SalesAnalysisService salesAnalysisService,
                              TimeAnalysisService timeAnalysisService,
                              ReportExportService reportExportService,
                              RestaurantConfigService configService,
                              PointOfSaleService pointOfSaleService,
                              ReportFormatter formatter,
                              BillValidationService billValidationService,
                              FoodCostService foodCostService) {
        this.salesAnalysisService = salesAnalysisService;
        this.timeAnalysisService = timeAnalysisService;
        this.reportExportService = reportExportService;
        this.configService = configService;
        this.pointOfSaleService = pointOfSaleService;
        this.formatter = formatter;
        this.billValidationService = billValidationService;
        this.foodCostService = foodCostService;
    }
    
    /**
     * Uruchamia główne menu aplikacji
     */
    public void run() {
        System.out.println("🍽️  WITAJ W SYSTEMIE ANALIZY RESTAURACJI! 🍽️");
        System.out.println("=" .repeat(50));
        
        while (true) {
            showMainMenu();
            int choice = getIntInput("Wybierz opcję (1-11): ");
            
            switch (choice) {
                case 1 -> generateSalesReport();
                case 2 -> analyzeHourlySales();
                case 3 -> generateYearlySummary();
                case 4 -> exportReports();
                case 5 -> validateSuspiciousBills();
                case 6 -> showConfiguration();
                case 7 -> showPointsOfSale();
                case 8 -> compareSalesReports();
                case 9 -> calculateKitchenPurchases();
                case 10 -> calculateFoodCost();
                case 11 -> {
                    System.out.println("👋 Dziękujemy za korzystanie z systemu!");
                    return;
                }
                default -> System.out.println("❌ Nieprawidłowy wybór. Spróbuj ponownie.");
            }
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Naciśnij Enter, aby kontynuować...");
            scanner.nextLine();
        }
    }
    
    private void showMainMenu() {
        System.out.println("\n📋 GŁÓWNE MENU:");
        System.out.println("1. 📊 Generuj raport sprzedaży");
        System.out.println("2. ⏰ Analiza sprzedaży godzinowej");
        System.out.println("3. 📅 Podsumowanie roczne");
        System.out.println("4. 📤 Eksport raportów");
        System.out.println("5. 🔍 Walidacja podejrzanych rachunków");
        System.out.println("6. ⚙️  Konfiguracja");
        System.out.println("7. 🏪 Punkty sprzedaży");
        System.out.println("8. 🔁 Porównanie dwóch okresów");
        System.out.println("9. 🧾 Zakupy (podsumowanie)");
        System.out.println("10. 💰 Food Cost (zakupy vs sprzedaż)");
        System.out.println("11. 🚪 Wyjście");
    }
    
    private void generateSalesReport() {
        System.out.println("\n📊 GENEROWANIE RAPORTU SPRZEDAŻY");
        System.out.println("-".repeat(40));

        // Wybór typu raportu: miesięczny vs dowolny okres
        System.out.println("\nWybierz typ raportu:");
        System.out.println("1. Raport dzienny dla miesiąca");
        System.out.println("2. Raport dzienny dla dowolnego okresu");
        int reportType = getIntInput("Wybierz opcję (1-2): ");

        LocalDate from;
        LocalDate to;

        if (reportType == 1) {
            int year = getIntInput("Podaj rok (np. 2025): ");
            int month = getIntInput("Podaj miesiąc (1-12): ");
            from = LocalDate.of(year, month, 1);
            to = from.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            System.out.println("📅 Zakres miesiąca: " + from + " - " + to);
        } else {
            from = getDateInput("Data początkowa (YYYY-MM-DD): ");
            to = getDateInput("Data końcowa (YYYY-MM-DD): ");
        }
        
        List<Integer> sellerIds = chooseSellerIds();
        
        try {
            RestaurantReportDto report = salesAnalysisService.generateSalesReport(from, to, sellerIds);
            System.out.println(formatter.formatRestaurantReport(report));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas generowania raportu: " + e.getMessage());
        }
    }

    private List<Integer> chooseSellerIds() {
        System.out.println("\nWybierz sprzedawców:");
        var kdSellers = pointOfSaleService.getPointOfSale("KD")
                .map(PointOfSale::getSellerIds)
                .map(java.util.ArrayList::new)
                .orElseGet(() -> new java.util.ArrayList<>(configService.getDefaultSellers()));
        var ratuszowaSellers = pointOfSaleService.getPointOfSale("RATUSZOWA")
                .map(PointOfSale::getSellerIds)
                .map(java.util.ArrayList::new)
                .orElseGet(() -> new java.util.ArrayList<>(configService.getAllSellers()));
        var allSellers = configService.getAllSellers();

        System.out.println("1. Kuchnia Domowa (sprzedawcy: " + kdSellers + ")");
        System.out.println("2. Ratuszowa (sprzedawcy: " + ratuszowaSellers + ")");
        System.out.println("3. Wszyscy (" + allSellers + ")");
        System.out.println("4. Własny wybór");

        return switch (getIntInput("Wybierz opcję (1-4): ")) {
            case 1 -> kdSellers;
            case 2 -> ratuszowaSellers;
            case 3 -> allSellers;
            case 4 -> getCustomSellerIds();
            default -> {
                System.out.println("❌ Nieprawidłowy wybór. Używam Kuchni Domowej.");
                yield kdSellers;
            }
        };
    }
    
    private void analyzeHourlySales() {
        System.out.println("\n⏰ ANALIZA SPRZEDAŻY GODZINOWEJ");
        System.out.println("-".repeat(40));
        
        LocalDate date = getDateInput("Data do analizy (YYYY-MM-DD): ");
        
        try {
            List<ObrotSprzedawcyGodzina> hourlyData = timeAnalysisService.analyzeHourlySales(date);
            System.out.println(formatter.formatHourlyAnalysis(hourlyData));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas analizy godzinowej: " + e.getMessage());
        }
    }
    
    private void generateYearlySummary() {
        System.out.println("\n📅 PODSUMOWANIE ROCZNE");
        System.out.println("-".repeat(40));
        
        int year = getIntInput("Podaj rok (np. 2024): ");
        
        try {
            var yearlyData = timeAnalysisService.generateYearlySummary(year);
            System.out.println(formatter.formatYearlySummary(yearlyData));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas generowania podsumowania rocznego: " + e.getMessage());
        }
    }
    
    private void exportReports() {
        System.out.println("\n📤 EKSPORT RAPORTÓW");
        System.out.println("-".repeat(40));
        
        LocalDate from = getDateInput("Data początkowa (YYYY-MM-DD): ");
        LocalDate to = getDateInput("Data końcowa (YYYY-MM-DD): ");
        List<Integer> sellerIds = configService.getDefaultSellers();
        
        System.out.println("\nWybierz format eksportu:");
        System.out.println("1. XML");
        System.out.println("2. CSV");
        System.out.println("3. Oba");
        
        int formatChoice = getIntInput("Wybierz opcję (1-3): ");
        
        try {
            RestaurantReportDto report = salesAnalysisService.generateSalesReport(from, to, sellerIds);
            
            switch (formatChoice) {
                case 1 -> {
                    reportExportService.exportToXml(report);
                    System.out.println("✅ Raport XML został wyeksportowany");
                }
                case 2 -> {
                    reportExportService.exportToCsv(report);
                    System.out.println("✅ Raport CSV został wyeksportowany");
                }
                case 3 -> {
                    reportExportService.exportToXml(report);
                    reportExportService.exportToCsv(report);
                    System.out.println("✅ Raporty XML i CSV zostały wyeksportowane");
                }
                default -> System.out.println("❌ Nieprawidłowy wybór formatu");
            }
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas eksportu: " + e.getMessage());
        }
    }
    
    private void showConfiguration() {
        System.out.println("\n⚙️  KONFIGURACJA RESTAURACJI");
        System.out.println("-".repeat(40));
        
        System.out.println("🏠 Grupy towarów kuchennych:");
        System.out.println("   " + configService.getKitchenGroups());
        
        System.out.println("\n👥 Domyślni sprzedawcy:");
        System.out.println("   " + configService.getDefaultSellers());
        
        System.out.println("\n👥 Wszyscy sprzedawcy:");
        System.out.println("   " + configService.getAllSellers());
    }
    
    private void showPointsOfSale() {
        System.out.println("\n🏪 PUNKTY SPRZEDAŻY");
        System.out.println("-".repeat(40));
        
        var allPoints = pointOfSaleService.getAllPointsOfSale();
        
        for (PointOfSale pos : allPoints) {
            System.out.println("\n📍 " + pos.getNazwa() + " (ID: " + pos.getId() + ")");
            System.out.println("   ID Użytkownika: " + 
                    (pos.getIdUzytkownika() != null ? pos.getIdUzytkownika() : "Brak (wielu sprzedawców)"));
            System.out.println("   Sprzedawcy: " + pos.getSellerIds());
            
            System.out.println("   Godziny pracy:");
            var workingHours = pos.getWorkingHours().getHoursByDay();
            String[] dniTygodnia = {"Niedziela", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota"};
            for (var entry : workingHours.entrySet()) {
                var range = entry.getValue();
                int dayIndex = entry.getKey().getValue() % 7;
                System.out.println("      " + dniTygodnia[dayIndex] + ": " + 
                        range.openTime() + " - " + range.closeTime() + 
                        " (" + range.getHours() + "h)");
            }
            
            System.out.println("   Kategorie sprzedaży: " + pos.getCategories());
        }
    }

    /**
     * Porównuje dwa dowolne okresy dla wybranych sprzedawców (paragony, obrót, AOV, udział kuchni).
     */
    private void compareSalesReports() {
        System.out.println("\n🔁 PORÓWNANIE DWÓCH OKRESÓW");
        System.out.println("-".repeat(40));

        System.out.println("\nOkres A:");
        LocalDate fromA = getDateInput("Data początkowa A (YYYY-MM-DD): ");
        LocalDate toA = getDateInput("Data końcowa A (YYYY-MM-DD): ");

        System.out.println("\nOkres B:");
        LocalDate fromB = getDateInput("Data początkowa B (YYYY-MM-DD): ");
        LocalDate toB = getDateInput("Data końcowa B (YYYY-MM-DD): ");

        List<Integer> sellerIds = chooseSellerIds();

        try {
            RestaurantReportDto reportA = salesAnalysisService.generateSalesReport(fromA, toA, sellerIds);
            RestaurantReportDto reportB = salesAnalysisService.generateSalesReport(fromB, toB, sellerIds);
            System.out.println(formatter.formatComparisonReport(reportA, reportB));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas porównywania raportów: " + e.getMessage());
        }
    }

    private void calculateKitchenPurchases() {
        System.out.println("\n🧾 ZAKUPY");
        System.out.println("-".repeat(40));

        System.out.println("\nWybierz magazyn:");
        var kitchenWarehouses = configService.getKitchenWarehouses();
        var buffetWarehouses = configService.getBuffetWarehouses();
        
        System.out.println("1. 🍳 Kuchnia (magazyny: " + kitchenWarehouses + ")");
        System.out.println("2. 🥤 Bufet (magazyny: " + buffetWarehouses + ")");
        
        int warehouseChoice = getIntInput("Wybierz opcję (1-2): ");
        
        List<Integer> selectedWarehouses;
        String warehouseName;
        
        switch (warehouseChoice) {
            case 1:
                if (kitchenWarehouses.isEmpty()) {
                    System.err.println("❌ Brak skonfigurowanych magazynów kuchni!");
                    return;
                }
                selectedWarehouses = kitchenWarehouses;
                warehouseName = "Kuchnia";
                break;
            case 2:
                if (buffetWarehouses.isEmpty()) {
                    System.err.println("❌ Brak skonfigurowanych magazynów bufetu!");
                    return;
                }
                selectedWarehouses = buffetWarehouses;
                warehouseName = "Bufet";
                break;
            default:
                System.err.println("❌ Nieprawidłowy wybór. Używam Kuchni.");
                if (kitchenWarehouses.isEmpty()) {
                    return;
                }
                selectedWarehouses = kitchenWarehouses;
                warehouseName = "Kuchnia";
                break;
        }

        System.out.println("\nWybierz okres:");
        System.out.println("1. Cały miesiąc");
        System.out.println("2. Dowolny zakres");
        int periodChoice = getIntInput("Wybierz opcję (1-2): ");

        LocalDate from;
        LocalDate to;

        if (periodChoice == 1) {
            int year = getIntInput("Podaj rok (np. 2025): ");
            int month = getIntInput("Podaj miesiąc (1-12): ");
            from = LocalDate.of(year, month, 1);
            to = from.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            System.out.println("📅 Zakres miesiąca: " + from + " - " + to);
        } else {
            from = getDateInput("Data początkowa (YYYY-MM-DD): ");
            to = getDateInput("Data końcowa (YYYY-MM-DD): ");
        }

        try {
            var summary = foodCostService.calculateWarehousePurchases(from, to, selectedWarehouses, warehouseName);
            System.out.println(formatter.formatKitchenPurchasesSummary(summary));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas wyliczania zakupów: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void calculateFoodCost() {
        System.out.println("\n💰 FOOD COST");
        System.out.println("-".repeat(40));

        System.out.println("\nWybierz magazyn:");
        System.out.println("1. 🍳 Kuchnia");
        System.out.println("2. 🥤 Bufet");
        int warehouseChoice = getIntInput("Wybierz opcję (1-2): ");

        String warehouseName;
        switch (warehouseChoice) {
            case 1 -> warehouseName = "Kuchnia";
            case 2 -> warehouseName = "Bufet";
            default -> {
                System.err.println("❌ Nieprawidłowy wybór. Używam Kuchni.");
                warehouseName = "Kuchnia";
            }
        }

        System.out.println("\nWybierz sprzedawców:");
        System.out.println("1. Kuchnia Domowa");
        System.out.println("2. Ratuszowa");
        System.out.println("3. Wszyscy");
        System.out.println("4. Własny wybór");
        int sellerChoice = getIntInput("Wybierz opcję (1-4): ");

        List<Integer> selectedSellers;
        switch (sellerChoice) {
            case 1 -> {
                var kd = pointOfSaleService.getPointOfSale("KD");
                selectedSellers = kd.map(PointOfSale::getSellerIds).orElse(configService.getDefaultSellers());
            }
            case 2 -> {
                var ratuszowa = pointOfSaleService.getPointOfSale("Ratuszowa");
                selectedSellers = ratuszowa.map(PointOfSale::getSellerIds).orElse(configService.getAllSellers());
            }
            case 3 -> selectedSellers = configService.getAllSellers();
            case 4 -> {
                System.out.print("Podaj ID sprzedawców (oddzielone przecinkami, np. 11,12,13): ");
                String input = scanner.nextLine().trim();
                selectedSellers = Arrays.stream(input.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList();
            }
            default -> {
                System.err.println("❌ Nieprawidłowy wybór. Używam domyślnych sprzedawców.");
                selectedSellers = configService.getDefaultSellers();
            }
        }

        System.out.println("\nWybierz okres:");
        System.out.println("1. Cały miesiąc");
        System.out.println("2. Dowolny zakres");
        int periodChoice = getIntInput("Wybierz opcję (1-2): ");

        LocalDate from;
        LocalDate to;

        if (periodChoice == 1) {
            int year = getIntInput("Podaj rok (np. 2025): ");
            int month = getIntInput("Podaj miesiąc (1-12): ");
            from = LocalDate.of(year, month, 1);
            to = from.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            System.out.println("📅 Zakres miesiąca: " + from + " - " + to);
        } else {
            from = getDateInput("Data początkowa (YYYY-MM-DD): ");
            to = getDateInput("Data końcowa (YYYY-MM-DD): ");
        }

        try {
            FoodCostSummary summary;
            if ("Kuchnia".equals(warehouseName)) {
                summary = foodCostService.calculateFoodCostForKitchen(from, to, selectedSellers);
            } else {
                summary = foodCostService.calculateFoodCostForBuffet(from, to, selectedSellers);
            }
            System.out.println(formatter.formatFoodCostSummary(summary, warehouseName));
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas obliczania food cost: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Metody pomocnicze
    
    private LocalDate getDateInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                System.out.println("❌ Data nie może być pusta");
                continue;
            }
            
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Nieprawidłowy format daty. Użyj YYYY-MM-DD (np. 2025-07-15)");
            }
        }
    }
    
    private int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ Nieprawidłowa liczba. Spróbuj ponownie.");
            }
        }
    }
    
    private List<Integer> getCustomSellerIds() {
        System.out.println("Podaj ID sprzedawców oddzielone przecinkami (np. 1,2,3): ");
        String input = scanner.nextLine().trim();
        
        try {
            return Arrays.stream(input.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            System.out.println("❌ Nieprawidłowy format. Używam domyślnych sprzedawców.");
            return configService.getDefaultSellers();
        }
    }
    
    private void validateSuspiciousBills() {
        System.out.println("\n🔍 WALIDACJA PODEJRZANYCH RACHUNKÓW");
        System.out.println("-".repeat(40));
        
        LocalDate from = getDateInput("Data początkowa (YYYY-MM-DD): ");
        LocalDate to = getDateInput("Data końcowa (YYYY-MM-DD): ");
        
        try {
            var suspiciousBills = billValidationService.findSuspiciousBills(from, to);
            var stats = billValidationService.getStats(suspiciousBills);
            
            if (suspiciousBills.isEmpty()) {
                System.out.println("✅ Brak podejrzanych rachunków w wybranym okresie!");
            } else {
                System.out.println("⚠️  Znaleziono " + suspiciousBills.size() + " podejrzanych rachunków!");
                System.out.println("\n📊 STATYSTYKI:");
                System.out.println("• Łączna kwota: " + stats.totalAmount() + " zł");
                System.out.println("• Bardzo podejrzane: " + stats.verySuspiciousCount());
                System.out.println("• Wysokie kwoty: " + stats.highAmountCount());
                System.out.println("• Bardzo krótki czas: " + stats.shortDurationCount());
                
                System.out.println("\n🔍 SZCZEGÓŁY:");
                for (var bill : suspiciousBills) {
                    System.out.println(String.format("• Rachunek #%d: %s zł, %s, %s - %s [%s] - %s",
                            bill.billId(),
                            bill.amount(),
                            bill.sellerName(),
                            bill.getDurationFormatted(),
                            bill.reason(),
                            bill.severity(),
                            bill.startTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Błąd podczas walidacji: " + e.getMessage());
        }
    }
}


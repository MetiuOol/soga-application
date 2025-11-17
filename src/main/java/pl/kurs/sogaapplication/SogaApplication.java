package pl.kurs.sogaapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pl.kurs.sogaapplication.models.business.PointOfSale;
import pl.kurs.sogaapplication.service.cli.CommandLineInterface;
import pl.kurs.sogaapplication.service.config.PointOfSaleService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class SogaApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SogaApplication.class, args);
        
        // Sprawdź czy są argumenty z linii komend
        if (args.length > 0 && args[0].equals("--cli")) {
            // Uruchom interfejs CLI
            CommandLineInterface cli = ctx.getBean(CommandLineInterface.class);
            cli.run();
        } else {
            // Uruchom przykładowe analizy (tryb demo)
            runDemoMode(ctx);
        }
    }

    private static void runDemoMode(ConfigurableApplicationContext ctx) {
        System.out.println("🍽️  SYSTEM ANALIZY RESTAURACJI - TRYB DEMO");
        System.out.println("=".repeat(60));
        System.out.println("Uruchom z parametrem --cli aby otworzyć interfejs użytkownika");
        System.out.println("Przykład: java -jar soga-application.jar --cli");
        System.out.println();
        System.out.flush(); // Wymuś wyświetlenie

        try {
            System.out.println("⏳ Inicjalizacja serwisów...");
            var analysisService = ctx.getBean(pl.kurs.sogaapplication.service.RestaurantAnalysisService.class);
            var formatter = ctx.getBean(pl.kurs.sogaapplication.service.display.ReportFormatter.class);
            var pointOfSaleService = ctx.getBean(PointOfSaleService.class);
            var configService = ctx.getBean(RestaurantConfigService.class);
            System.out.println("✅ Serwisy zainicjalizowane\n");
            System.out.flush();

            // 0. DIAGNOSTYKA: Sprawdź grupy towarów kuchennych
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔍 DIAGNOSTYKA: GRUPY TOWARÓW KUCHENNYCH");
            System.out.println("=".repeat(60));
            try {
                checkKitchenGroups(ctx);
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas sprawdzania grup towarów: " + e.getMessage());
                e.printStackTrace();
            }

            // 1. POKAŻ PUNKTY SPRZEDAŻY
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🏪 PUNKTY SPRZEDAŻY");
            System.out.println("=".repeat(60));
            try {
                showPointsOfSale(pointOfSaleService);
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas wyświetlania punktów sprzedaży: " + e.getMessage());
                e.printStackTrace();
            }

            // 2. POKAŻ KONFIGURACJĘ
            System.out.println("\n" + "=".repeat(60));
            System.out.println("⚙️  KONFIGURACJA");
            System.out.println("=".repeat(60));
            try {
                showConfiguration(configService);
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas wyświetlania konfiguracji: " + e.getMessage());
                e.printStackTrace();
            }

            // 3. PRZYKŁADOWY RAPORT - STYCZEŃ 2025 (wszyscy sprzedawcy)
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 PRZYKŁADOWY RAPORT - STYCZEŃ 2025 (Wszyscy sprzedawcy)");
            System.out.println("=".repeat(60));
            try {
                System.out.println("⏳ Generowanie raportu...");
                var sellerIds = analysisService.getAllSellerIds();
                System.out.println("📋 Sprzedawcy: " + sellerIds);
                var reportAll = analysisService.generateRestaurantReport(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31),
                        sellerIds
                );
                System.out.println(formatter.formatRestaurantReport(reportAll));
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas generowania raportu dla wszystkich sprzedawców: " + e.getMessage());
                e.printStackTrace();
                System.err.flush();
            }

            // 4. PRZYKŁADOWY RAPORT - STYCZEŃ 2025 (tylko KD)
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 PRZYKŁADOWY RAPORT - STYCZEŃ 2025 (Tylko Kuchnia Domowa)");
            System.out.println("=".repeat(60));
            try {
                System.out.println("⏳ Generowanie raportu KD...");
                var defaultSellers = analysisService.getDefaultSellerIds();
                System.out.println("📋 Sprzedawcy: " + defaultSellers);
                var reportKD = analysisService.generateRestaurantReport(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31),
                        defaultSellers
                );
                System.out.println(formatter.formatRestaurantReport(reportKD));
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas generowania raportu dla KD: " + e.getMessage());
                e.printStackTrace();
                System.err.flush();
            }

            // 5. ANALIZA GODZINOWA - przykładowy dzień
            System.out.println("\n" + "=".repeat(60));
            System.out.println("⏰ ANALIZA GODZINOWA - 15 STYCZNIA 2025");
            System.out.println("=".repeat(60));
            try {
                System.out.println("⏳ Analizowanie godzinowej sprzedaży...");
                var timeAnalysisService = ctx.getBean(pl.kurs.sogaapplication.service.analysis.TimeAnalysisService.class);
                var hourlyData = timeAnalysisService.analyzeHourlySales(LocalDate.of(2025, 1, 15));
                System.out.println(formatter.formatHourlyAnalysis(hourlyData));
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas analizy godzinowej: " + e.getMessage());
                e.printStackTrace();
                System.err.flush();
            }

            // 6. WALIDACJA PODEJRZANYCH RACHUNKÓW
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔍 WALIDACJA PODEJRZANYCH RACHUNKÓW - STYCZEŃ 2025");
            System.out.println("=".repeat(60));
            try {
                System.out.println("⏳ Wyszukiwanie podejrzanych rachunków...");
                var billValidationService = ctx.getBean(pl.kurs.sogaapplication.service.validation.BillValidationService.class);
                var suspiciousBills = billValidationService.findSuspiciousBills(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 31)
                );
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
                    
                    System.out.println("\n🔍 PIERWSZE 5 RACHUNKÓW:");
                    suspiciousBills.stream()
                            .limit(5)
                            .forEach(bill -> System.out.println(String.format(
                                    "  • Rachunek #%d: %s zł, %s, %s [%s] - %s",
                                    bill.billId(),
                                    bill.amount(),
                                    bill.sellerName(),
                                    bill.getDurationFormatted(),
                                    bill.severity(),
                                    bill.startTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                            )));
                }
                System.out.flush();
            } catch (Exception e) {
                System.err.println("❌ Błąd podczas walidacji: " + e.getMessage());
                e.printStackTrace();
                System.err.flush();
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅ TRYB DEMO ZAKOŃCZONY");
            System.out.println("=".repeat(60));
            System.out.println("\n💡 Aby użyć interfejsu użytkownika, uruchom aplikację z parametrem --cli");
            System.out.flush();

        } catch (Exception e) {
            System.err.println("❌ Błąd podczas uruchamiania trybu demo: " + e.getMessage());
            e.printStackTrace();
            System.err.flush();
        }
    }

    private static void showPointsOfSale(PointOfSaleService pointOfSaleService) {
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

    private static void showConfiguration(RestaurantConfigService configService) {
        System.out.println("🏠 Towary kuchenne (ID_TW):");
        System.out.println("   " + configService.getKitchenProducts());

        System.out.println("\n🏠 Grupy towarów kuchennych (stara metoda, opcjonalne):");
        System.out.println("   " + configService.getKitchenGroups());
        
        System.out.println("\n👥 Domyślni sprzedawcy:");
        System.out.println("   " + configService.getDefaultSellers());
        
        System.out.println("\n👥 Wszyscy sprzedawcy:");
        System.out.println("   " + configService.getAllSellers());
    }

    private static void checkKitchenGroups(ConfigurableApplicationContext ctx) {
        var configService = ctx.getBean(RestaurantConfigService.class);
        var entityManager = ctx.getBean(jakarta.persistence.EntityManager.class);
        
        Integer kuchniaDomowaId = 11; // ID Kuchni Domowej
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 2, 1);
        
        System.out.println("🔎 Analiza TOWARÓW używanych przez Kuchnię Domową (sprzedawca ID=" + kuchniaDomowaId + ")");
        System.out.println("📅 Okres: " + from + " - " + to);
        System.out.println();
        System.out.println("⚠️  UWAGA: To są WSZYSTKIE towary sprzedawane przez KD w tym okresie!");
        System.out.println("   Musisz SAM zdecydować, które to 'KUCHNIA' a które 'BUFET'.");
        System.out.println("   (Kuchnia = dania/zupy/sosy przygotowywane w kuchni, Bufet = napoje/dodatki bez gotowania)");
        System.out.println();
        
        try {
            @SuppressWarnings("unchecked")
            var products = entityManager.createNativeQuery("""
                    SELECT
                        t.ID_TW,
                        t.NAZWA_TW,
                        t.ID_GR,
                        COALESCE(SUM(
                            CASE WHEN p.NR_POZ_KOR > 0
                                 THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                                 ELSE p.WART_NU
                            END
                        ), 0) AS PRZYCHOD_NETTO,
                        COALESCE(SUM(p.ILOSC), 0) AS ILOSC
                    FROM POZRACH p
                    JOIN RACHUNKI r   ON r.ID_RACH = p.ID_RACH
                    LEFT JOIN POZRACH parent
                           ON parent.ID_RACH = p.ID_RACH
                          AND parent.NR_POZ  = p.NR_POZ
                          AND parent.NR_POZ_KOR = 0
                    JOIN TOWARY t     ON t.ID_TW = p.ID_TW
                    WHERE r.ID_UZ = :sellerId
                      AND r.DATA_ROZ >= :from
                      AND r.DATA_ROZ <  :to
                    GROUP BY t.ID_TW, t.NAZWA_TW, t.ID_GR
                    ORDER BY PRZYCHOD_NETTO DESC
                    """)
                    .setParameter("sellerId", kuchniaDomowaId)
                    .setParameter("from", from.atStartOfDay())
                    .setParameter("to", to.atStartOfDay())
                    .getResultList();

            System.out.println("🔍 Wynik zapytania: znaleziono " + products.size() + " rekordów");
            
            if (products.isEmpty()) {
                System.out.println("⚠️  Nie znaleziono żadnych towarów dla Kuchni Domowej w tym okresie!");
                System.out.println("\n💡 Możliwe przyczyny:");
                System.out.println("   - Brak transakcji dla sprzedawcy ID=" + kuchniaDomowaId + " w tym okresie");
                System.out.println("   - Wszystkie towary mają NULL w polu ID_GR");
                System.out.println("   - Problem z JOIN między tabelami");
                return;
            }
            
            System.out.println("📊 Lista towarów (posortowana po przychodzie netto):");
            System.out.println("-".repeat(120));
            System.out.printf("%-8s %-40s %-8s %-20s %-15s%n",
                    "ID_TW", "Nazwa towaru", "ID_GR", "Przychód netto", "Ilość");
            System.out.println("-".repeat(100));
            
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
            java.util.List<Long> foundProductIds = new java.util.ArrayList<>();
            
            for (int i = 0; i < products.size(); i++) {
                Object[] row = (Object[]) products.get(i);
                
                try {
                    Long productId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    String productName = row[1] != null ? row[1].toString() : "(brak nazwy)";
                    Integer groupId = row[2] != null ? ((Number) row[2]).intValue() : null;
                    java.math.BigDecimal przychod = row[3] != null
                            ? (java.math.BigDecimal) row[3]
                            : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal ilosc = row[4] != null
                            ? (java.math.BigDecimal) row[4]
                            : java.math.BigDecimal.ZERO;

                    if (productId != null) {
                        foundProductIds.add(productId);
                        System.out.printf("%-8d %-40s %-8s %-20s %-15s%n",
                                productId,
                                productName.length() > 40 ? productName.substring(0, 37) + "..." : productName,
                                groupId != null ? groupId.toString() : "-",
                                df.format(przychod) + " zł",
                                df.format(ilosc)
                        );
                    }
                } catch (Exception e) {
                    System.err.println("❌ Błąd podczas parsowania rzędu " + i + ": " + e.getMessage());
                }
            }
            
            System.out.println("-".repeat(100));
            
            // Porównanie z konfiguracją
            var configuredProducts = configService.getKitchenProducts();
            System.out.println("\n⚙️  Aktualna konfiguracja w application.properties:");
            System.out.println("   restaurant.kitchen.products=" + configuredProducts);
            
            System.out.println("\n💡 REKOMENDACJA:");
            System.out.println("   1. Przejrzyj powyższą listę towarów.");
            System.out.println("   2. Zaznacz towary, które są KUCHNIA (dania, zupy, sosy, itp.).");
            System.out.println("   3. Z ich ID_TW zbuduj listę i wpisz ją w application.properties:");
            System.out.println("      restaurant.kitchen.products=ID1,ID2,ID3,...");
            System.out.println("   4. Pozostałe towary traktujemy jako BUFET.");
            System.out.println();
            System.out.println("   ❗ NIE wpisuj tu wszystkich towarów – tylko te, które faktycznie wychodzą z kuchni!");
            
            if (!foundProductIds.isEmpty()) {
                System.out.println("\n📋 Wszystkie znalezione ID_TW (do ręcznego wyboru):");
                System.out.println("   " + foundProductIds);
            }
        } catch (Exception e) {
            System.err.println("❌ Błąd podczas wykonywania zapytania: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
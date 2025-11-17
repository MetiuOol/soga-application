package pl.kurs.sogaapplication.service.display;

import org.springframework.stereotype.Component;
import pl.kurs.sogaapplication.dto.DokumentZakupuDto;
import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.KitchenPurchasesSummary;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Pozycja;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.service.core.RachunekService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serwis do formatowania raportów w czytelny sposób
 */
@Component
public class ReportFormatter {
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    /**
     * Formatuje raport food cost (zakupy vs sprzedaż kuchni).
     */
    public String formatFoodCostSummary(FoodCostSummary summary) {
        return formatFoodCostSummary(summary, "Kuchnia");
    }

    public String formatFoodCostSummary(FoodCostSummary summary, String category) {
        StringBuilder sb = new StringBuilder();

        String title = "Kuchnia".equals(category) ? "🥘 FOOD COST – KUCHNIA" : "🥤 FOOD COST – BUFET";
        String salesLabel = "Kuchnia".equals(category) ? "🍳 Sprzedaż kuchni netto" : "🥤 Sprzedaż bufetu netto";
        String warehouseLabel = "Kuchnia".equals(category) ? "🏬 Magazyny kuchni (ID_MA)" : "🏬 Magazyny bufetu (ID_MA)";
        
        sb.append(title).append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append(String.format("📅 Okres: %s - %s\n",
                summary.from().format(DATE_FORMAT),
                summary.to().format(DATE_FORMAT)));
        sb.append(String.format("👥 Sprzedawcy: %s\n", summary.sellerIds()));
        sb.append(String.format("%s: %s\n", warehouseLabel, summary.warehouseIds()));
        sb.append("\n");

        sb.append(String.format("%s:      %15s\n", salesLabel, CURRENCY_FORMAT.format(summary.kitchenSalesNet())));
        sb.append(String.format("🧾 Zakupy netto FZ:            %15s\n", CURRENCY_FORMAT.format(summary.purchasesFzNet())));
        sb.append(String.format("📄 Zakupy netto PZ (bez FZ):   %15s\n", CURRENCY_FORMAT.format(summary.purchasesPzNet())));
        sb.append(String.format("🧮 Zakupy netto łącznie:       %15s\n", CURRENCY_FORMAT.format(summary.purchasesTotalNet())));
        sb.append("\n");
        sb.append(String.format("📉 Food cost:                  %15s%%\n", NUMBER_FORMAT.format(summary.foodCostPercent())));
        sb.append(String.format("   (zakupy / sprzedaż × 100)\n"));
        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    /**
     * Formatuje proste podsumowanie zakupów kuchni (bez porównania ze sprzedażą).
     */
    public String formatKitchenPurchasesSummary(KitchenPurchasesSummary summary) {
        StringBuilder sb = new StringBuilder();

        sb.append("🧾 ZAKUPY ").append(summary.warehouseName().toUpperCase()).append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append(String.format("📅 Okres: %s - %s\n",
                summary.from().format(DATE_FORMAT),
                summary.to().format(DATE_FORMAT)));
        sb.append(String.format("🏬 Magazyny %s (ID_MA): %s\n", summary.warehouseName(), summary.warehouseIds()));
        sb.append("\n");

        sb.append(String.format("🧾 Zakupy netto FZ:            %15s\n", CURRENCY_FORMAT.format(summary.purchasesFzNet())));
        sb.append(String.format("📄 Zakupy netto PZ (bez FZ):   %15s\n", CURRENCY_FORMAT.format(summary.purchasesPzNet())));
        sb.append(String.format("📝 Zakupy netto KFZ:           %15s\n", CURRENCY_FORMAT.format(summary.purchasesKfzNet())));
        String mmpLabel = "Kuchnia".equals(summary.warehouseName()) 
                ? "MMP (bufet→kuchnia)" 
                : "MMP (kuchnia→bufet)";
        String mmLabel = "Kuchnia".equals(summary.warehouseName()) 
                ? "MM (kuchnia→bufet)" 
                : "MM (bufet→kuchnia)";
        sb.append(String.format("🔄 Zakupy netto MMP (%s): %15s\n", mmpLabel, CURRENCY_FORMAT.format(summary.purchasesMmpNet())));
        sb.append(String.format("⬅️  Przeniesienia MM (%s): %15s\n", mmLabel, CURRENCY_FORMAT.format(summary.purchasesMmNet())));
        sb.append(String.format("🧮 Zakupy netto łącznie:       %15s\n", CURRENCY_FORMAT.format(summary.purchasesTotalNet())));
        sb.append("\n");

        // Lista dokumentów
        if (summary.dokumenty().isEmpty()) {
            sb.append("📋 Brak dokumentów w wybranym okresie.\n");
        } else {
            sb.append("📋 LISTA DOKUMENTÓW (").append(summary.dokumenty().size()).append("):\n");
            sb.append("-".repeat(80)).append("\n");
            sb.append(String.format("%-8s %-4s %-8s %-12s %-6s %-12s %-20s %-12s\n",
                    "ID_DOK", "TYP", "ID_POCH", "NR_ORYGIN", "ID_FI", "DATA_WST", "CALY_NR", "WART_NU"));
            sb.append("-".repeat(80)).append("\n");

            for (DokumentZakupuDto doc : summary.dokumenty()) {
                sb.append(String.format("%-8s %-4s %-8s %-12s %-6s %-12s %-20s %-12s\n",
                        doc.idDok() != null ? doc.idDok().toString() : "",
                        doc.typDok() != null ? doc.typDok() : "",
                        doc.idPochodzenia() != null ? doc.idPochodzenia().toString() : "",
                        doc.nrOryginalny() != null ? (doc.nrOryginalny().length() > 12 ? doc.nrOryginalny().substring(0, 12) : doc.nrOryginalny()) : "",
                        doc.idFirmy() != null ? doc.idFirmy().toString() : "",
                        doc.dataWst() != null ? doc.dataWst().format(DATE_FORMAT) : "",
                        doc.calyNumer() != null ? (doc.calyNumer().length() > 20 ? doc.calyNumer().substring(0, 20) : doc.calyNumer()) : "",
                        CURRENCY_FORMAT.format(doc.wartNu())));
            }
            sb.append("-".repeat(80)).append("\n");
        }

        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    private final RachunekService rachunekService;

    public ReportFormatter(RachunekService rachunekService) {
        this.rachunekService = rachunekService;
    }
    
    /**
     * Formatuje główny raport restauracji
     */
    public String formatRestaurantReport(RestaurantReportDto report) {
        StringBuilder sb = new StringBuilder();
        
        // Nagłówek
        sb.append("=".repeat(80)).append("\n");
        sb.append("                    RAPORT SPRZEDAŻY RESTAURACJI\n");
        sb.append("=".repeat(80)).append("\n");
        
        // Informacje podstawowe
        sb.append(String.format("📅 Okres: %s - %s\n", 
                report.from().format(DATE_FORMAT), 
                report.to().format(DATE_FORMAT)));
        sb.append(String.format("👥 Sprzedawcy: %s\n", report.sellerIds()));
        sb.append(String.format("📊 Liczba dni: %d\n", report.dailySales().size()));
        sb.append("\n");
        
        // Podsumowanie finansowe
        sb.append("💰 PODSUMOWANIE FINANSOWE:\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("🍳 Kuchnia:       %15s\n", CURRENCY_FORMAT.format(report.kitchenSales())));
        sb.append(String.format("🥤 Bufet:         %15s\n", CURRENCY_FORMAT.format(report.buffetSales())));
        sb.append(String.format("📦 Opakowania:    %15s\n", CURRENCY_FORMAT.format(report.packagingSales())));
        sb.append(String.format("🚚 Dowóz:         %15s\n", CURRENCY_FORMAT.format(report.deliverySales())));
        sb.append(String.format("📈 Razem:         %15s\n", CURRENCY_FORMAT.format(report.totalSales())));
        sb.append("\n");
        
        // Statystyki
        formatStatistics(sb, report);
        
        // Najlepsze i najgorsze dni
        formatBestWorstDays(sb, report);
        
        // Tabela dzienna
        formatDailyTable(sb, report);
        
        // Sekcja podejrzanych rachunków
        formatSuspiciousBills(sb, report);
        
        sb.append("=".repeat(80)).append("\n");
        
        return sb.toString();
    }

    /**
     * Raport porównawczy dwóch okresów (A vs B) dla tych samych sprzedawców.
     * Pokazuje: paragony, obrót, AOV, udział kuchni.
     */
    public String formatComparisonReport(RestaurantReportDto periodA, RestaurantReportDto periodB) {
        StringBuilder sb = new StringBuilder();

        // Wylicz podstawowe liczby
        long billsA = rachunekService.countByRangeAndSellers(periodA.from(), periodA.to(), periodA.sellerIds());
        long billsB = rachunekService.countByRangeAndSellers(periodB.from(), periodB.to(), periodB.sellerIds());

        BigDecimal totalA = periodA.totalSales();
        BigDecimal totalB = periodB.totalSales();

        BigDecimal aovA = billsA > 0
                ? totalA.divide(BigDecimal.valueOf(billsA), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal aovB = billsB > 0
                ? totalB.divide(BigDecimal.valueOf(billsB), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal kitchenPercentA = totalA.compareTo(BigDecimal.ZERO) > 0
                ? periodA.kitchenSales().divide(totalA, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        BigDecimal kitchenPercentB = totalB.compareTo(BigDecimal.ZERO) > 0
                ? periodB.kitchenSales().divide(totalB, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Nagłówek
        sb.append("📊 PORÓWNANIE DWÓCH OKRESÓW\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append(String.format("Sprzedawcy: %s\n\n", periodA.sellerIds()));

        sb.append(String.format("%-12s %-22s %-12s %-16s %-16s\n",
                "", "Okres", "Paragony", "Obrót netto", "AOV"));
        sb.append("-".repeat(80)).append("\n");

        sb.append(String.format("%-12s %-22s %-12d %-16s %-16s\n",
                "Okres A",
                periodA.from() + " - " + periodA.to(),
                billsA,
                CURRENCY_FORMAT.format(totalA),
                CURRENCY_FORMAT.format(aovA)));

        sb.append(String.format("%-12s %-22s %-12d %-16s %-16s\n",
                "Okres B",
                periodB.from() + " - " + periodB.to(),
                billsB,
                CURRENCY_FORMAT.format(totalB),
                CURRENCY_FORMAT.format(aovB)));

        sb.append("-".repeat(80)).append("\n");

        // Zmiany
        long deltaBills = billsB - billsA;
        BigDecimal deltaTotal = totalB.subtract(totalA);
        BigDecimal deltaAov = aovB.subtract(aovA);

        sb.append("🔄 ZMIANY (B vs A):\n");
        sb.append(String.format("Paragony:   %+d\n", deltaBills));

        String deltaTotalStr = (deltaTotal.signum() >= 0 ? "+" : "") + CURRENCY_FORMAT.format(deltaTotal);
        String deltaAovStr = (deltaAov.signum() >= 0 ? "+" : "") + CURRENCY_FORMAT.format(deltaAov);

        sb.append(String.format("Obrót:      %s\n", deltaTotalStr));
        sb.append(String.format("AOV:        %s\n", deltaAovStr));
        sb.append(String.format("Kuchnia%% A: %.1f%%   Kuchnia%% B: %.1f%%\n",
                kitchenPercentA, kitchenPercentB));

        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }
    
    /**
     * Formatuje analizę godzinową
     */
    public String formatHourlyAnalysis(List<ObrotSprzedawcyGodzina> hourlyData) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("⏰ ANALIZA GODZINOWA SPRZEDAŻY\n");
        sb.append("=".repeat(60)).append("\n");
        
        // Grupuj po sprzedawcach
        Map<String, List<ObrotSprzedawcyGodzina>> bySeller = hourlyData.stream()
                .collect(Collectors.groupingBy(ObrotSprzedawcyGodzina::sellerName));
        
        for (Map.Entry<String, List<ObrotSprzedawcyGodzina>> entry : bySeller.entrySet()) {
            String sellerName = entry.getKey();
            List<ObrotSprzedawcyGodzina> sellerData = entry.getValue();
            
            sb.append(String.format("\n👤 Sprzedawca: %s\n", sellerName));
            sb.append("-".repeat(40)).append("\n");
            
            // Sortuj po godzinach
            sellerData.stream()
                    .sorted((a, b) -> Short.compare(a.godzina(), b.godzina()))
                    .forEach(data -> sb.append(String.format("  %02d:00  —  %12s\n", 
                            data.godzina(), CURRENCY_FORMAT.format(data.suma()))));
            
            // Suma dla sprzedawcy
            BigDecimal sellerTotal = sellerData.stream()
                    .map(ObrotSprzedawcyGodzina::suma)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sb.append(String.format("  Razem: %12s\n", CURRENCY_FORMAT.format(sellerTotal)));
        }
        
        return sb.toString();
    }
    
    /**
     * Formatuje podsumowanie roczne
     */
    public String formatYearlySummary(List<pl.kurs.sogaapplication.dto.PodsumowanieTygodnia> yearlyData) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("📅 PODSUMOWANIE ROCZNE\n");
        sb.append("=".repeat(80)).append("\n");
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalBills = 0;
        int totalPeople = 0;
        
        sb.append(String.format("%-8s %-12s %-10s %-8s %-12s\n", 
                "Tydzień", "Sprzedaż", "Rachunki", "Osoby", "Średnia/bill"));
        sb.append("-".repeat(80)).append("\n");
        
        for (var week : yearlyData) {
            BigDecimal avgPerBill = week.getLiczbaRachunkow() > 0 
                    ? week.getSumaNetto().divide(BigDecimal.valueOf(week.getLiczbaRachunkow()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            sb.append(String.format("%-8d %-12s %-10d %-8d %-12s\n",
                    week.getNumerTygodnia(),
                    CURRENCY_FORMAT.format(week.getSumaNetto()),
                    week.getLiczbaRachunkow(),
                    week.getLacznaIloscOsob(),
                    CURRENCY_FORMAT.format(avgPerBill)));
            
            totalRevenue = totalRevenue.add(week.getSumaNetto());
            totalBills += week.getLiczbaRachunkow();
            totalPeople += week.getLacznaIloscOsob();
        }
        
        sb.append("-".repeat(80)).append("\n");
        sb.append(String.format("%-8s %-12s %-10d %-8d %-12s\n",
                "RAZEM",
                CURRENCY_FORMAT.format(totalRevenue),
                totalBills,
                totalPeople,
                totalBills > 0 ? CURRENCY_FORMAT.format(totalRevenue.divide(BigDecimal.valueOf(totalBills), 2, RoundingMode.HALF_UP)) : "0,00 zł"));
        
        return sb.toString();
    }
    
    // Metody pomocnicze
    
    private void formatStatistics(StringBuilder sb, RestaurantReportDto report) {
        sb.append("📊 STATYSTYKI:\n");
        sb.append("-".repeat(40)).append("\n");
        
        if (!report.dailySales().isEmpty()) {
            BigDecimal avgDaily = report.totalSales().divide(BigDecimal.valueOf(report.dailySales().size()), 2, RoundingMode.HALF_UP);
            sb.append(String.format("Średnia dzienna:    %15s\n", CURRENCY_FORMAT.format(avgDaily)));
            
            // Procentowy udział poszczególnych kategorii
            if (report.totalSales().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal kitchenPercent = report.kitchenSales().divide(report.totalSales(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                BigDecimal buffetPercent = report.buffetSales().divide(report.totalSales(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                BigDecimal packagingPercent = report.packagingSales().divide(report.totalSales(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                BigDecimal deliveryPercent = report.deliverySales().divide(report.totalSales(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                
                sb.append(String.format("Kuchnia:            %15s (%.1f%%)\n", 
                        CURRENCY_FORMAT.format(report.kitchenSales()), kitchenPercent));
                sb.append(String.format("Bufet:              %15s (%.1f%%)\n", 
                        CURRENCY_FORMAT.format(report.buffetSales()), buffetPercent));
                sb.append(String.format("Opakowania:         %15s (%.1f%%)\n",
                        CURRENCY_FORMAT.format(report.packagingSales()), packagingPercent));
                sb.append(String.format("Dowóz:              %15s (%.1f%%)\n",
                        CURRENCY_FORMAT.format(report.deliverySales()), deliveryPercent));
            }
        }
        sb.append("\n");
    }
    
    private void formatBestWorstDays(StringBuilder sb, RestaurantReportDto report) {
        if (report.dailySales().isEmpty()) return;
        
        var bestDay = report.dailySales().stream()
                .max((a, b) -> a.total().compareTo(b.total()))
                .orElse(null);
        var worstDay = report.dailySales().stream()
                .min((a, b) -> a.total().compareTo(b.total()))
                .orElse(null);
        
        if (bestDay != null && worstDay != null) {
            sb.append("🏆 NAJLEPSZE I NAJGORSZE DNI:\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(String.format("🥇 Najlepszy: %s - %s\n", 
                    bestDay.date().format(DATE_FORMAT), CURRENCY_FORMAT.format(bestDay.total())));
            sb.append(String.format("🥉 Najgorszy: %s - %s\n", 
                    worstDay.date().format(DATE_FORMAT), CURRENCY_FORMAT.format(worstDay.total())));
            sb.append("\n");
        }
    }
    
    private void formatDailyTable(StringBuilder sb, RestaurantReportDto report) {
        sb.append("📅 SZCZEGÓŁY DZIENNE:\n");
        sb.append("-".repeat(110)).append("\n");
        sb.append(String.format("%-15s %-15s %-15s %-15s %-15s %-15s %-10s %-8s\n",
                "Data", "Kuchnia", "Bufet", "Opakowania", "Dowóz", "Razem", "Paragony", "Kuchnia%"));
        sb.append("-".repeat(110)).append("\n");
        
        // Sumy do wyświetlenia na końcu
        BigDecimal totalKitchen = BigDecimal.ZERO;
        BigDecimal totalBuffet = BigDecimal.ZERO;
        BigDecimal totalPackaging = BigDecimal.ZERO;
        BigDecimal totalDelivery = BigDecimal.ZERO;
        BigDecimal totalSum = BigDecimal.ZERO;
        long totalBills = 0;
        
        for (var daily : report.dailySales()) {
            long billCount = 0;
            try {
                billCount = rachunekService.countByDateAndSellers(daily.date(), report.sellerIds());
            } catch (Exception ignored) {
                // Jeśli z jakiegoś powodu nie uda się pobrać liczby rachunków, zostaw 0
            }

            BigDecimal kitchenPercent = daily.total().compareTo(BigDecimal.ZERO) > 0 
                    ? daily.kitchen().divide(daily.total(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            
            sb.append(String.format("%-15s %-15s %-15s %-15s %-15s %-15s %-10d %-8s\n",
                    formatDateWithDayOfWeek(daily.date()),
                    CURRENCY_FORMAT.format(daily.kitchen()),
                    CURRENCY_FORMAT.format(daily.buffet()),
                    CURRENCY_FORMAT.format(daily.packaging()),
                    CURRENCY_FORMAT.format(daily.delivery()),
                    CURRENCY_FORMAT.format(daily.total()),
                    billCount,
                    String.format("%.1f%%", kitchenPercent)));
            
            // Dodaj do sum
            totalKitchen = totalKitchen.add(daily.kitchen());
            totalBuffet = totalBuffet.add(daily.buffet());
            totalPackaging = totalPackaging.add(daily.packaging());
            totalDelivery = totalDelivery.add(daily.delivery());
            totalSum = totalSum.add(daily.total());
            totalBills += billCount;
        }
        
        // Linia separatora i sumy
        sb.append("-".repeat(90)).append("\n");
        
        // Oblicz średni procent kuchni
        BigDecimal avgKitchenPercent = totalSum.compareTo(BigDecimal.ZERO) > 0 
                ? totalKitchen.divide(totalSum, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        sb.append(String.format("%-15s %-15s %-15s %-15s %-15s %-15s %-10d %-8s\n",
                "SUMA:",
                CURRENCY_FORMAT.format(totalKitchen),
                CURRENCY_FORMAT.format(totalBuffet),
                CURRENCY_FORMAT.format(totalPackaging),
                CURRENCY_FORMAT.format(totalDelivery),
                CURRENCY_FORMAT.format(totalSum),
                totalBills,
                String.format("%.1f%%", avgKitchenPercent)));
    }
    
    /**
     * Formatuje sekcję podejrzanych rachunków
     */
    private void formatSuspiciousBills(StringBuilder sb, RestaurantReportDto report) {
        if (report.suspiciousBills() == null || report.suspiciousBills().isEmpty()) {
            sb.append("\n✅ WALIDACJA RACHUNKÓW: Brak podejrzanych rachunków\n");
            return;
        }
        
        sb.append("\n⚠️  PODEJRZANE RACHUNKI:\n");
        sb.append("=".repeat(80)).append("\n");
        
        // Statystyki
        var stats = report.suspiciousStats();
        sb.append(String.format("📊 Statystyki: %d podejrzanych rachunków (%.2f zł)\n", 
                stats.totalCount(), stats.totalAmount()));
        sb.append(String.format("   • Bardzo podejrzane: %d\n", stats.verySuspiciousCount()));
        sb.append(String.format("   • Wysokie kwoty: %d\n", stats.highAmountCount()));
        sb.append(String.format("   • Bardzo krótki czas: %d\n", stats.shortDurationCount()));
        sb.append("\n");
        
        // Szczegóły rachunków
        sb.append("🔍 SZCZEGÓŁY:\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append(String.format("%-8s %-15s %-20s %-12s %-15s %s\n",
                "ID", "Kwota", "Sprzedawca", "Czas", "Data", "Powód"));
        sb.append("-".repeat(80)).append("\n");
        
        // Grupuj po poziomie zagrożenia
        var billsBySeverity = report.suspiciousBills().stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.severity(),
                        Collectors.toList()
                ));
        
        // Wyświetl najpierw najbardziej podejrzane
        String[] severityOrder = {"BARDZO PODEJRZANY", "PODEJRZANY", "WYSOKA KWOTA", "BARDZO KRÓTKI CZAS", "BŁĄD DATY"};
        
        for (String severity : severityOrder) {
            var bills = billsBySeverity.get(severity);
            if (bills != null && !bills.isEmpty()) {
                String severityIcon = getSeverityIcon(severity);
                sb.append(String.format("\n%s %s (%d rachunków):\n", severityIcon, severity, bills.size()));
                
                for (var bill : bills) {
                    sb.append(String.format("  %-6d %-15s %-20s %-12s %-15s %s\n",
                            bill.billId(),
                            CURRENCY_FORMAT.format(bill.amount()),
                            bill.sellerName(),
                            bill.getDurationFormatted(),
                            bill.startTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                            bill.reason()));
                    // Pod spodem wypisz zawartość rachunku (pozycje)
                    appendBillItems(sb, bill.billId());
                }
            }
        }
        
        // Ostrzeżenie jeśli są bardzo podejrzane rachunki
        if (stats.verySuspiciousCount() > 0) {
            sb.append("\n🚨 UWAGA: Znaleziono " + stats.verySuspiciousCount() + 
                    " bardzo podejrzanych rachunków! Sprawdź czy nie są to błędy w systemie.\n");
        }
    }

    /**
     * Dla podanego rachunku wypisuje jego pozycje (towary) pod linią z nagłówkiem.
     */
    private void appendBillItems(StringBuilder sb, Long billId) {
        try {
            Rachunek rachunek = rachunekService.findByIdWithUserAndPositions(billId);
            if (rachunek.getPozycje() == null || rachunek.getPozycje().isEmpty()) {
                sb.append("     (brak pozycji na rachunku)\n");
                return;
            }

            sb.append("     Zawartość rachunku:\n");
            for (Pozycja poz : rachunek.getPozycje()) {
                var towar = poz.getTowar();
                Long towarId = null;
                String towarNazwa = "(niezaładowany)";
                if (towar != null) {
                    if (towar instanceof org.hibernate.proxy.HibernateProxy proxy) {
                        towarId = (Long) proxy.getHibernateLazyInitializer().getIdentifier();
                    } else {
                        towarId = towar.getIdTowaru();
                    }
                    if (org.hibernate.Hibernate.isInitialized(towar)) {
                        towarNazwa = towar.getNazwaTowaru();
                    }
                }

                String iloscStr = poz.getIloscTowaru() != null
                        ? NUMBER_FORMAT.format(poz.getIloscTowaru())
                        : "1";

                sb.append(String.format("       • ID_TW=%s  %s  x %s\n",
                        towarId != null ? towarId : "?",
                        towarNazwa != null ? towarNazwa : "(brak nazwy)",
                        iloscStr));
            }
        } catch (Exception e) {
            sb.append("     (nie udało się pobrać pozycji rachunku: ").append(e.getMessage()).append(")\n");
        }
    }
    
    /**
     * Zwraca ikonę dla poziomu zagrożenia
     */
    private String getSeverityIcon(String severity) {
        return switch (severity) {
            case "BARDZO PODEJRZANY" -> "🚨";
            case "PODEJRZANY" -> "⚠️";
            case "WYSOKA KWOTA" -> "💰";
            case "BARDZO KRÓTKI CZAS" -> "⏰";
            case "BŁĄD DATY" -> "📅";
            default -> "❓";
        };
    }
    
    /**
     * Formatuje datę z dniem tygodnia w języku polskim
     */
    private String formatDateWithDayOfWeek(java.time.LocalDate date) {
        String dayOfWeek = getPolishDayOfWeek(date.getDayOfWeek());
        return String.format("%s %s", dayOfWeek, date.format(DATE_FORMAT));
    }
    
    /**
     * Zwraca dzień tygodnia w języku polskim
     */
    private String getPolishDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Pn";
            case TUESDAY -> "Wt";
            case WEDNESDAY -> "Śr";
            case THURSDAY -> "Cz";
            case FRIDAY -> "Pt";
            case SATURDAY -> "Sb";
            case SUNDAY -> "Nd";
        };
    }
}

package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO dla raportu marży brutto dziennej.
 * Pokazuje dla każdego dnia sprzedaż, koszty żywności, koszty ogólne i marżę brutto/netto.
 */
public record DailyGrossMarginDto(
        LocalDate date,
        BigDecimal totalSales,
        BigDecimal kitchenSales,
        BigDecimal buffetSales,
        BigDecimal packagingSales,
        BigDecimal deliverySales,
        BigDecimal kitchenCost,
        BigDecimal buffetCost,
        BigDecimal totalCost, // koszty żywności (kuchnia + bufet)
        BigDecimal costs, // koszty ogólne (magazyn koszty) - średnia dzienna kumulatywna od początku roku
        BigDecimal grossMargin, // marża brutto = sprzedaż - koszty żywności
        BigDecimal netMargin, // marża netto = marża brutto - koszty ogólne
        boolean isProfit // true jeśli marża netto > 0
) {
    
    /**
     * Summary dla całego miesiąca.
     */
    public record MonthlySummary(
            LocalDate from,
            LocalDate to,
            String pointOfSale,
            List<Integer> sellerIds,
            BigDecimal kitchenFoodCostPercent,
            BigDecimal buffetFoodCostPercent,
            List<DailyGrossMarginDto> dailyMargins,
            long profitDays,
            long lossDays,
            BigDecimal totalSales,
            BigDecimal totalCost, // koszty żywności łącznie
            BigDecimal totalCosts, // koszty ogólne łącznie
            BigDecimal totalGrossMargin, // marża brutto łączna
            BigDecimal totalNetMargin, // marża netto łączna
            BigDecimal averageDailyMargin, // średnia marża netto dzienna
            DailyGrossMarginDto bestDay,
            DailyGrossMarginDto worstDay
    ) {}
}



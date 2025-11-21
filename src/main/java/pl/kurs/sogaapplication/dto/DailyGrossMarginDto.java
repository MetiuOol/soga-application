package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO dla raportu marży brutto dziennej.
 * Pokazuje dla każdego dnia sprzedaż, koszty żywności i marżę brutto.
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
        BigDecimal totalCost,
        BigDecimal grossMargin,
        boolean isProfit // true jeśli marża > 0
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
            BigDecimal totalCost,
            BigDecimal totalGrossMargin,
            BigDecimal averageDailyMargin,
            DailyGrossMarginDto bestDay,
            DailyGrossMarginDto worstDay
    ) {}
}



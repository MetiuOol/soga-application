package pl.kurs.sogaapplication.dto;

import pl.kurs.sogaapplication.models.SuspiciousBill;
import pl.kurs.sogaapplication.service.validation.BillValidationService.SuspiciousBillStats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO dla raportów restauracji
 */
public record RestaurantReportDto(
        LocalDate from,
        LocalDate to,
        List<Integer> sellerIds,
        KitchenBuffetSales kitchenBuffet,
        List<DailySales> dailySales,
        BigDecimal totalSales,
        BigDecimal kitchenSales,
        BigDecimal buffetSales,
        BigDecimal packagingSales,
        BigDecimal deliverySales,
        List<SuspiciousBill> suspiciousBills,
        SuspiciousBillStats suspiciousStats
) {
    
    public record KitchenBuffetSales(
            BigDecimal kitchen,
            BigDecimal buffet,
            BigDecimal packaging,
            BigDecimal delivery,
            BigDecimal total
    ) {}
    
    public record DailySales(
            LocalDate date,
            BigDecimal kitchen,
            BigDecimal buffet,
            BigDecimal packaging,
            BigDecimal delivery,
            BigDecimal total
    ) {}
    
    public static RestaurantReportDto create(LocalDate from, LocalDate to, List<Integer> sellerIds,
                                           KitchenBuffetSales kitchenBuffet, List<DailySales> dailySales,
                                           List<SuspiciousBill> suspiciousBills, SuspiciousBillStats suspiciousStats) {
        BigDecimal totalKitchen = dailySales.stream()
                .map(DailySales::kitchen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalBuffet = dailySales.stream()
                .map(DailySales::buffet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPackaging = dailySales.stream()
                .map(DailySales::packaging)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDelivery = dailySales.stream()
                .map(DailySales::delivery)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new RestaurantReportDto(
                from, to, sellerIds, kitchenBuffet, dailySales,
                totalKitchen.add(totalBuffet).add(totalPackaging).add(totalDelivery),
                totalKitchen, totalBuffet, totalPackaging, totalDelivery,
                suspiciousBills, suspiciousStats
        );
    }
}


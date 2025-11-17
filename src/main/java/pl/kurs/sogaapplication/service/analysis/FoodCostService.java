package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.DokumentZakupuDto;
import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.KitchenPurchasesSummary;
import pl.kurs.sogaapplication.repositories.DokumentJpaRepository;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.List;

@Service
public class FoodCostService {

    private static final Logger log = LoggerFactory.getLogger(FoodCostService.class);

    private final DokumentJpaRepository dokumentRepository;
    private final RachunekJpaRepository rachunekRepository;
    private final RestaurantConfigService configService;

    public FoodCostService(DokumentJpaRepository dokumentRepository,
                           RachunekJpaRepository rachunekRepository,
                           RestaurantConfigService configService) {
        this.dokumentRepository = dokumentRepository;
        this.rachunekRepository = rachunekRepository;
        this.configService = configService;
    }

    /**
     * Wylicza food cost dla kuchni w zadanym miesiącu.
     *
     * @param year       rok
     * @param month      miesiąc (1-12)
     * @param sellerIds  sprzedawcy przypisani do lokalizacji (KD, Ratuszowa, etc.)
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateMonthlyFoodCost(int year, int month, Collection<Integer> sellerIds) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.with(TemporalAdjusters.lastDayOfMonth());
        return calculateFoodCost(from, to, sellerIds);
    }

    /**
     * Główna metoda licząca food cost w zadanym zakresie dat (włącznie).
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCost(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var warehouseIds = configService.getKitchenWarehouses();
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów kuchni (restaurant.warehouses.kitchen)");
        }

        var kitchenProducts = configService.getKitchenProducts();
        if (kitchenProducts.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych produktów kuchni (restaurant.kitchen.products)");
        }

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        BigDecimal kitchenSales = rachunekRepository.sumaKuchniaBySellers(
                fromDateTime,
                toDateTime,
                kitchenProducts,
                sellerIds
        );

        LocalDate toExclusive = to.plusDays(1);
        BigDecimal purchasesFz = dokumentRepository.sumFzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal purchasesPz = dokumentRepository.sumStandalonePzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal totalPurchases = purchasesFz.add(purchasesPz);

        BigDecimal foodCostPercent = kitchenSales.signum() == 0
                ? BigDecimal.ZERO
                : totalPurchases
                .divide(kitchenSales, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Food cost {} - {} | sprzedawcy {} | magazyny {} | sprzedaż {} | zakupy {}",
                from, to, sellerIds, warehouseIds, kitchenSales, totalPurchases);

        return new FoodCostSummary(
                from,
                to,
                List.copyOf(sellerIds),
                List.copyOf(warehouseIds),
                kitchenSales,
                purchasesFz,
                purchasesPz,
                totalPurchases,
                foodCostPercent
        );
    }

    /**
     * Proste podsumowanie zakupów kuchni z wybranego okresu (bez porównania ze sprzedażą).
     */
    @Transactional(readOnly = true)
    public KitchenPurchasesSummary calculateKitchenPurchases(LocalDate from, LocalDate to) {
        var warehouseIds = configService.getKitchenWarehouses();
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów kuchni (restaurant.warehouses.kitchen)");
        }

        LocalDate toExclusive = to.plusDays(1);
        BigDecimal purchasesFz = dokumentRepository.sumFzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal purchasesPz = dokumentRepository.sumStandalonePzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal purchasesKfz = dokumentRepository.sumKfzNettoByWarehouses(from, toExclusive, warehouseIds);
        
        // MMP: przeniesienia z bufetu (9) do kuchni (8)
        var buffetWarehouses = configService.getBuffetWarehouses();
        Integer buffetWarehouseId = buffetWarehouses.isEmpty() ? 9 : buffetWarehouses.get(0); // domyślnie 9
        Integer kitchenWarehouseId = warehouseIds.get(0); // pierwszy magazyn kuchni (domyślnie 8)
        BigDecimal purchasesMmp = dokumentRepository.sumMmpNettoByWarehouses(from, toExclusive, kitchenWarehouseId, buffetWarehouseId);
        
        // MM: przeniesienia z kuchni (8) - odejmujemy od zakupów (ID_MA_2 dowolny)
        BigDecimal purchasesMm = dokumentRepository.sumMmNettoByWarehouses(from, toExclusive, kitchenWarehouseId);
        
        BigDecimal totalPurchases = purchasesFz.add(purchasesPz).add(purchasesKfz).add(purchasesMmp).subtract(purchasesMm);

        // Pobierz listę dokumentów
        List<Object[]> fzDocs = dokumentRepository.findFzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        List<Object[]> pzDocs = dokumentRepository.findStandalonePzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        List<Object[]> kfzDocs = dokumentRepository.findKfzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        List<Object[]> mmpDocs = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, kitchenWarehouseId, buffetWarehouseId);
        List<Object[]> mmDocs = dokumentRepository.findMmDocumentsByWarehouses(from, toExclusive, kitchenWarehouseId);
        
        List<DokumentZakupuDto> dokumenty = new java.util.ArrayList<>();
        
        // Mapuj FZ
        for (Object[] row : fzDocs) {
            LocalDate dataWst;
            if (row[5] instanceof java.sql.Date) {
                dataWst = ((java.sql.Date) row[5]).toLocalDate();
            } else if (row[5] instanceof java.time.LocalDate) {
                dataWst = (java.time.LocalDate) row[5];
            } else {
                dataWst = ((java.sql.Timestamp) row[5]).toLocalDateTime().toLocalDate();
            }
            
            dokumenty.add(new DokumentZakupuDto(
                    ((Number) row[0]).longValue(),      // ID_DOK
                    (String) row[1],                     // TYP_DOK
                    row[2] != null ? ((Number) row[2]).longValue() : null,  // ID_POCHOD
                    row[3] != null ? (String) row[3] : null,               // NR_ORYGIN
                    row[4] != null ? ((Number) row[4]).intValue() : null,  // ID_FI
                    dataWst,                            // DATA_WST
                    row[6] != null ? (String) row[6] : null,               // CALY_NR
                    ((java.math.BigDecimal) row[7])                        // WART_NU
            ));
        }
        
        // Mapuj PZ
        for (Object[] row : pzDocs) {
            LocalDate dataWst;
            if (row[5] instanceof java.sql.Date) {
                dataWst = ((java.sql.Date) row[5]).toLocalDate();
            } else if (row[5] instanceof java.time.LocalDate) {
                dataWst = (java.time.LocalDate) row[5];
            } else {
                dataWst = ((java.sql.Timestamp) row[5]).toLocalDateTime().toLocalDate();
            }
            
            dokumenty.add(new DokumentZakupuDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    row[2] != null ? ((Number) row[2]).longValue() : null,
                    row[3] != null ? (String) row[3] : null,
                    row[4] != null ? ((Number) row[4]).intValue() : null,
                    dataWst,
                    row[6] != null ? (String) row[6] : null,
                    ((java.math.BigDecimal) row[7])
            ));
        }
        
        // Mapuj KFZ
        for (Object[] row : kfzDocs) {
            LocalDate dataWst;
            if (row[5] instanceof java.sql.Date) {
                dataWst = ((java.sql.Date) row[5]).toLocalDate();
            } else if (row[5] instanceof java.time.LocalDate) {
                dataWst = (java.time.LocalDate) row[5];
            } else {
                dataWst = ((java.sql.Timestamp) row[5]).toLocalDateTime().toLocalDate();
            }
            
            dokumenty.add(new DokumentZakupuDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    row[2] != null ? ((Number) row[2]).longValue() : null,
                    row[3] != null ? (String) row[3] : null,
                    row[4] != null ? ((Number) row[4]).intValue() : null,
                    dataWst,
                    row[6] != null ? (String) row[6] : null,
                    ((java.math.BigDecimal) row[7])
            ));
        }
        
        // Mapuj MMP
        for (Object[] row : mmpDocs) {
            LocalDate dataWst;
            if (row[5] instanceof java.sql.Date) {
                dataWst = ((java.sql.Date) row[5]).toLocalDate();
            } else if (row[5] instanceof java.time.LocalDate) {
                dataWst = (java.time.LocalDate) row[5];
            } else {
                dataWst = ((java.sql.Timestamp) row[5]).toLocalDateTime().toLocalDate();
            }
            
            dokumenty.add(new DokumentZakupuDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    row[2] != null ? ((Number) row[2]).longValue() : null,
                    row[3] != null ? (String) row[3] : null,
                    row[4] != null ? ((Number) row[4]).intValue() : null,
                    dataWst,
                    row[6] != null ? (String) row[6] : null,
                    ((java.math.BigDecimal) row[7])
            ));
        }
        
        // Mapuj MM (dokumenty odejmowane - przeniesienia z kuchni do bufetu)
        for (Object[] row : mmDocs) {
            LocalDate dataWst;
            if (row[5] instanceof java.sql.Date) {
                dataWst = ((java.sql.Date) row[5]).toLocalDate();
            } else if (row[5] instanceof java.time.LocalDate) {
                dataWst = (java.time.LocalDate) row[5];
            } else {
                dataWst = ((java.sql.Timestamp) row[5]).toLocalDateTime().toLocalDate();
            }
            
            dokumenty.add(new DokumentZakupuDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    row[2] != null ? ((Number) row[2]).longValue() : null,
                    row[3] != null ? (String) row[3] : null,
                    row[4] != null ? ((Number) row[4]).intValue() : null,
                    dataWst,
                    row[6] != null ? (String) row[6] : null,
                    ((java.math.BigDecimal) row[7]).negate() // wartość ujemna, bo odejmujemy
            ));
        }

        log.debug("Zakupy kuchni {} - {} | magazyny {} | FZ {} | PZ {} | KFZ {} | MMP {} | MM {} | razem {} | dokumentów: {}",
                from, to, warehouseIds, purchasesFz, purchasesPz, purchasesKfz, purchasesMmp, purchasesMm, totalPurchases, dokumenty.size());

        return new KitchenPurchasesSummary(
                from,
                to,
                List.copyOf(warehouseIds),
                purchasesFz,
                purchasesPz,
                purchasesKfz,
                purchasesMmp,
                purchasesMm,
                totalPurchases,
                dokumenty
        );
    }
}


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
     * Proste podsumowanie zakupów magazynu z wybranego okresu (bez porównania ze sprzedażą).
     * Obsługuje zarówno kuchnię, jak i bufet.
     */
    @Transactional(readOnly = true)
    public KitchenPurchasesSummary calculateWarehousePurchases(LocalDate from, LocalDate to, 
                                                               List<Integer> warehouseIds, String warehouseName) {
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów: " + warehouseName);
        }

        LocalDate toExclusive = to.plusDays(1);
        
        // Pobierz magazyny z konfiguracji dla przeniesień
        var kitchenWarehouses = configService.getKitchenWarehouses();
        var buffetWarehouses = configService.getBuffetWarehouses();
        Integer kitchenWarehouseId = kitchenWarehouses.isEmpty() ? 8 : kitchenWarehouses.get(0);
        Integer buffetWarehouseId = buffetWarehouses.isEmpty() ? 9 : buffetWarehouses.get(0);
        
        // Zakupy podstawowe: FZ, PZ, KFZ
        BigDecimal purchasesFz = dokumentRepository.sumFzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal purchasesPz = dokumentRepository.sumStandalonePzNettoByWarehouses(from, toExclusive, warehouseIds);
        BigDecimal purchasesKfz = dokumentRepository.sumKfzNettoByWarehouses(from, toExclusive, warehouseIds);
        
        BigDecimal purchasesMmp = BigDecimal.ZERO;
        BigDecimal purchasesMm = BigDecimal.ZERO;
        
        // MMP i MM zależą od wybranego magazynu
        if ("Kuchnia".equals(warehouseName)) {
            // Dla kuchni: MMP z bufetu do kuchni (dodawane), MM z kuchni (odejmowane)
            purchasesMmp = dokumentRepository.sumMmpNettoByWarehouses(from, toExclusive, kitchenWarehouseId, buffetWarehouseId);
            purchasesMm = dokumentRepository.sumMmNettoByWarehouses(from, toExclusive, kitchenWarehouseId);
        } else if ("Bufet".equals(warehouseName)) {
            // Dla bufetu: MMP z kuchni do bufetu (dodawane), MM z bufetu (odejmowane)
            purchasesMmp = dokumentRepository.sumMmpNettoByWarehouses(from, toExclusive, buffetWarehouseId, kitchenWarehouseId);
            purchasesMm = dokumentRepository.sumMmNettoByWarehouses(from, toExclusive, buffetWarehouseId);
        }
        
        BigDecimal totalPurchases = purchasesFz.add(purchasesPz).add(purchasesKfz).add(purchasesMmp).subtract(purchasesMm);

        // Pobierz listę dokumentów
        List<Object[]> fzDocs = dokumentRepository.findFzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        List<Object[]> pzDocs = dokumentRepository.findStandalonePzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        List<Object[]> kfzDocs = dokumentRepository.findKfzDocumentsByWarehouses(from, toExclusive, warehouseIds);
        
        List<Object[]> mmpDocs;
        List<Object[]> mmDocs;
        
        if ("Kuchnia".equals(warehouseName)) {
            mmpDocs = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, kitchenWarehouseId, buffetWarehouseId);
            mmDocs = dokumentRepository.findMmDocumentsByWarehouses(from, toExclusive, kitchenWarehouseId);
        } else {
            mmpDocs = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, buffetWarehouseId, kitchenWarehouseId);
            mmDocs = dokumentRepository.findMmDocumentsByWarehouses(from, toExclusive, buffetWarehouseId);
        }
        
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

        log.debug("Zakupy {} {} - {} | magazyny {} | FZ {} | PZ {} | KFZ {} | MMP {} | MM {} | razem {} | dokumentów: {}",
                warehouseName, from, to, warehouseIds, purchasesFz, purchasesPz, purchasesKfz, purchasesMmp, purchasesMm, totalPurchases, dokumenty.size());

        return new KitchenPurchasesSummary(
                from,
                to,
                warehouseName,
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

    /**
     * Proste podsumowanie zakupów kuchni z wybranego okresu (bez porównania ze sprzedażą).
     * Metoda pomocnicza dla kompatybilności wstecznej.
     */
    @Transactional(readOnly = true)
    public KitchenPurchasesSummary calculateKitchenPurchases(LocalDate from, LocalDate to) {
        var warehouseIds = configService.getKitchenWarehouses();
        return calculateWarehousePurchases(from, to, warehouseIds, "Kuchnia");
    }

    /**
     * Oblicza food cost dla kuchni: porównuje zakupy kuchni ze sprzedażą kuchni w danym okresie.
     * Używa wszystkich typów dokumentów zakupu (FZ, PZ, KFZ, MMP, MM) i całej sprzedaży kuchni.
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCostForKitchen(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var kitchenProducts = configService.getKitchenProducts();
        if (kitchenProducts.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych produktów kuchni (restaurant.kitchen.products)");
        }

        // Pobierz zakupy kuchni (z wszystkimi typami dokumentów)
        var warehouseIds = configService.getKitchenWarehouses();
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów kuchni (restaurant.warehouses.kitchen)");
        }
        var purchasesSummary = calculateWarehousePurchases(from, to, warehouseIds, "Kuchnia");

        // Pobierz sprzedaż kuchni
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
        BigDecimal kitchenSales = rachunekRepository.sumaKuchniaBySellers(
                fromDateTime,
                toDateTime,
                kitchenProducts,
                sellerIds
        );

        // Oblicz food cost %
        BigDecimal foodCostPercent = kitchenSales.signum() == 0
                ? BigDecimal.ZERO
                : purchasesSummary.purchasesTotalNet()
                .divide(kitchenSales, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Food cost kuchni {} - {} | sprzedawcy {} | magazyny {} | sprzedaż {} | zakupy {} | food cost {}%",
                from, to, sellerIds, warehouseIds, kitchenSales, purchasesSummary.purchasesTotalNet(), foodCostPercent);

        return new FoodCostSummary(
                from,
                to,
                List.copyOf(sellerIds),
                List.copyOf(warehouseIds),
                kitchenSales,
                purchasesSummary.purchasesFzNet(),
                purchasesSummary.purchasesPzNet(),
                purchasesSummary.purchasesTotalNet(),
                foodCostPercent
        );
    }

    /**
     * Oblicza food cost dla bufetu: porównuje zakupy bufetu ze sprzedażą bufetu w danym okresie.
     * Używa wszystkich typów dokumentów zakupu (FZ, PZ, KFZ, MMP, MM) i całej sprzedaży bufetu.
     * Sprzedaż bufetu = całkowita sprzedaż - sprzedaż kuchni - sprzedaż opakowań - sprzedaż dowozu.
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCostForBuffet(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var kitchenProducts = configService.getKitchenProducts();
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();

        // Pobierz zakupy bufetu (z wszystkimi typami dokumentów)
        var warehouseIds = configService.getBuffetWarehouses();
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów bufetu (restaurant.warehouses.buffet)");
        }
        var purchasesSummary = calculateWarehousePurchases(from, to, warehouseIds, "Bufet");

        // Pobierz sprzedaż bufetu
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
        
        BigDecimal totalSales = rachunekRepository.sumaRazemBySellers(fromDateTime, toDateTime, sellerIds);
        BigDecimal kitchenSales = (kitchenProducts == null || kitchenProducts.isEmpty())
                ? BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(fromDateTime, toDateTime, kitchenProducts, sellerIds);
        BigDecimal packagingSales = (packagingProducts == null || packagingProducts.isEmpty())
                ? BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(fromDateTime, toDateTime, packagingProducts, sellerIds);
        BigDecimal deliverySales = (deliveryProducts == null || deliveryProducts.isEmpty())
                ? BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(fromDateTime, toDateTime, deliveryProducts, sellerIds);
        
        BigDecimal buffetSales = totalSales
                .subtract(kitchenSales)
                .subtract(packagingSales)
                .subtract(deliverySales);

        // Oblicz food cost %
        BigDecimal foodCostPercent = buffetSales.signum() == 0
                ? BigDecimal.ZERO
                : purchasesSummary.purchasesTotalNet()
                .divide(buffetSales, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Food cost bufetu {} - {} | sprzedawcy {} | magazyny {} | sprzedaż bufetu {} | zakupy {} | food cost {}%",
                from, to, sellerIds, warehouseIds, buffetSales, purchasesSummary.purchasesTotalNet(), foodCostPercent);

        // Używamy FoodCostSummary, ale pole kitchenSalesNet reprezentuje w tym przypadku buffetSalesNet
        return new FoodCostSummary(
                from,
                to,
                List.copyOf(sellerIds),
                List.copyOf(warehouseIds),
                buffetSales, // w tym przypadku to buffetSales, nie kitchenSales
                purchasesSummary.purchasesFzNet(),
                purchasesSummary.purchasesPzNet(),
                purchasesSummary.purchasesTotalNet(),
                foodCostPercent
        );
    }
}


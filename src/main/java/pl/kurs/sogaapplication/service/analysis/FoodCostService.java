package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.DailyGrossMarginDto;
import pl.kurs.sogaapplication.dto.DokumentZakupuDto;
import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.KitchenPurchasesSummary;
import pl.kurs.sogaapplication.models.DzienPodzial;
import pl.kurs.sogaapplication.repositories.DokumentJpaRepository;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.analysis.SalesAnalysisService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class FoodCostService {

    private static final Logger log = LoggerFactory.getLogger(FoodCostService.class);

    private final DokumentJpaRepository dokumentRepository;
    private final RachunekJpaRepository rachunekRepository;
    private final RestaurantConfigService configService;
    private final SalesAnalysisService salesAnalysisService;
    private final java.util.Map<String, WarehouseFoodCostCalculator> calculators;

    public FoodCostService(DokumentJpaRepository dokumentRepository,
                           RachunekJpaRepository rachunekRepository,
                           RestaurantConfigService configService,
                           SalesAnalysisService salesAnalysisService,
                           java.util.List<WarehouseFoodCostCalculator> calculatorList) {
        this.dokumentRepository = dokumentRepository;
        this.rachunekRepository = rachunekRepository;
        this.configService = configService;
        this.salesAnalysisService = salesAnalysisService;
        
        // Tworzymy mapę kalkulatorów po nazwie magazynu
        this.calculators = calculatorList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        WarehouseFoodCostCalculator::getWarehouseName,
                        calc -> calc
                ));
    }
    
    /**
     * Pobiera kalkulator dla danego magazynu.
     */
    public WarehouseFoodCostCalculator getCalculator(String warehouseName) {
        WarehouseFoodCostCalculator calculator = calculators.get(warehouseName);
        if (calculator == null) {
            throw new IllegalArgumentException("Nie znaleziono kalkulatora dla magazynu: " + warehouseName);
        }
        return calculator;
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
        } else if ("Koszty".equals(warehouseName)) {
            // Dla kosztów: sprawdzamy przeniesienia do/z magazynu koszty
            var costsWarehouses = configService.getCostWarehouses();
            if (!costsWarehouses.isEmpty()) {
                Integer costsWarehouseId = costsWarehouses.get(0);
                
                // MMP: przeniesienia DO magazynu koszty (z innych magazynów)
                // Sprawdzamy wszystkie możliwe źródła: kuchnia, bufet
                BigDecimal mmpFromKitchen = dokumentRepository.sumMmpNettoByWarehouses(from, toExclusive, costsWarehouseId, kitchenWarehouseId);
                BigDecimal mmpFromBuffet = dokumentRepository.sumMmpNettoByWarehouses(from, toExclusive, costsWarehouseId, buffetWarehouseId);
                purchasesMmp = mmpFromKitchen.add(mmpFromBuffet);
                
                // MM: przeniesienia Z magazynu koszty (do innych magazynów)
                purchasesMm = dokumentRepository.sumMmNettoByWarehouses(from, toExclusive, costsWarehouseId);
            }
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
        } else if ("Bufet".equals(warehouseName)) {
            mmpDocs = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, buffetWarehouseId, kitchenWarehouseId);
            mmDocs = dokumentRepository.findMmDocumentsByWarehouses(from, toExclusive, buffetWarehouseId);
        } else if ("Koszty".equals(warehouseName)) {
            // Dla kosztów: pobierz przeniesienia do/z magazynu koszty
            var costsWarehouses = configService.getCostWarehouses();
            if (!costsWarehouses.isEmpty()) {
                Integer costsWarehouseId = costsWarehouses.get(0);
                
                // MMP: przeniesienia DO magazynu koszty (z kuchni i bufetu)
                List<Object[]> mmpFromKitchen = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, costsWarehouseId, kitchenWarehouseId);
                List<Object[]> mmpFromBuffet = dokumentRepository.findMmpDocumentsByWarehouses(from, toExclusive, costsWarehouseId, buffetWarehouseId);
                mmpDocs = new java.util.ArrayList<>();
                mmpDocs.addAll(mmpFromKitchen);
                mmpDocs.addAll(mmpFromBuffet);
                
                // MM: przeniesienia Z magazynu koszty
                mmDocs = dokumentRepository.findMmDocumentsByWarehouses(from, toExclusive, costsWarehouseId);
            } else {
                mmpDocs = Collections.emptyList();
                mmDocs = Collections.emptyList();
            }
        } else {
            mmpDocs = Collections.emptyList();
            mmDocs = Collections.emptyList();
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
     * Deleguje do KitchenFoodCostCalculator.
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCostForKitchen(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        return getCalculator("Kuchnia").calculateFoodCost(from, to, sellerIds);
    }

    /**
     * Oblicza food cost dla bufetu: porównuje zakupy bufetu ze sprzedażą bufetu w danym okresie.
     * Używa wszystkich typów dokumentów zakupu (FZ, PZ, KFZ, MMP, MM) i całej sprzedaży bufetu.
     * Sprzedaż bufetu = całkowita sprzedaż - sprzedaż kuchni - sprzedaż opakowań - sprzedaż dowozu.
     * Deleguje do BuffetFoodCostCalculator.
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCostForBuffet(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        return getCalculator("Bufet").calculateFoodCost(from, to, sellerIds);
    }
    
    /**
     * Oblicza food cost dla magazynu koszty.
     * Dla kosztów nie ma sprzedaży, więc zwraca tylko zakupy bez food cost %.
     * Deleguje do CostsFoodCostCalculator.
     */
    @Transactional(readOnly = true)
    public FoodCostSummary calculateFoodCostForCosts(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        return getCalculator("Koszty").calculateFoodCost(from, to, sellerIds);
    }

    /**
     * Oblicza marżę brutto dzienną dla wybranego miesiąca.
     * Dla każdego dnia oblicza sprzedaż, koszty żywności (na podstawie food cost % z miesiąca), 
     * koszty ogólne (kumulatywnie od początku roku) i marżę brutto/netto.
     * @param year Rok
     * @param month Miesiąc (1-12)
     * @param sellerIds Sprzedawcy dla których obliczamy marżę dzienną
     * @param foodCostSellerIds Sprzedawcy dla których obliczamy food cost % (zwykle wszyscy)
     * @param pointOfSaleName Nazwa punktu sprzedaży
     */
    @Transactional(readOnly = true)
    public DailyGrossMarginDto.MonthlySummary calculateDailyGrossMargin(int year, int month, 
                                                                        Collection<Integer> sellerIds,
                                                                        Collection<Integer> foodCostSellerIds,
                                                                        String pointOfSaleName) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.with(TemporalAdjusters.lastDayOfMonth());

        // Oblicz food cost % dla kuchni i bufetu z całego miesiąca (dla foodCostSellerIds)
        FoodCostSummary kitchenFoodCost = calculateFoodCostForKitchen(from, to, foodCostSellerIds);
        FoodCostSummary buffetFoodCost = calculateFoodCostForBuffet(from, to, foodCostSellerIds);

        BigDecimal kitchenFoodCostPercent = kitchenFoodCost.foodCostPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal buffetFoodCostPercent = buffetFoodCost.foodCostPercent()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Pobierz dzienną sprzedaż - użyjmy publicznej metody analyzeDaily
        // analyzeDaily przyjmuje pierwszy dzień miesiąca i sellerIds, zwraca listę dla całego miesiąca
        List<DzienPodzial> dailySales = salesAnalysisService.analyzeDailySales(from, sellerIds);

        // Cache dla kosztów kumulatywnych - klucz: rok, wartość: średnia dzienna kosztów
        Map<Integer, BigDecimal> yearlyAverageCostsCache = new HashMap<>();
        
        // Oblicz średnią dzienną kosztów dla wszystkich punktów (skumulowane koszty / wszystkie dni kalendarzowe)
        BigDecimal averageDailyCostsForAllPoints = calculateAverageDailyCostsForYear(year, yearlyAverageCostsCache);
        
        // Koszty całkowite miesiąca = średnia dzienna × liczba dni w miesiącu
        long daysInMonth = ChronoUnit.DAYS.between(from, to.plusDays(1));
        BigDecimal totalMonthlyCosts = averageDailyCostsForAllPoints.multiply(BigDecimal.valueOf(daysInMonth))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Oblicz proporcje sprzedaży dla tego punktu sprzedaży za CAŁY MIESIĄC
        BigDecimal monthlySalesShare = calculateMonthlySalesShareForPointOfSale(from, to, sellerIds);
        
        // Koszty miesiąca dla tego punktu = koszty całkowite miesiąca × udział w sprzedaży
        BigDecimal pointOfSaleMonthlyCosts = totalMonthlyCosts.multiply(monthlySalesShare)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Zlicz liczbę dni ze sprzedażą dla tego punktu w miesiącu
        LocalDateTime monthStartDateTime = from.atStartOfDay();
        LocalDateTime monthEndDateTime = to.plusDays(1).atStartOfDay();
        long daysWithSalesForPoint = rachunekRepository.countDaysWithSalesInDateRangeBySellers(
                monthStartDateTime, monthEndDateTime, sellerIds);
        
        // Koszty dzienne dla tego punktu = koszty miesiąca / liczba dni ze sprzedażą
        BigDecimal pointOfSaleDailyCosts = (daysWithSalesForPoint == 0)
                ? BigDecimal.ZERO
                : pointOfSaleMonthlyCosts.divide(BigDecimal.valueOf(daysWithSalesForPoint), 2, RoundingMode.HALF_UP);
        
        log.debug("Koszty dla punktu {} w miesiącu {}: średnia dzienna wszystkich punktów: {}, dni w miesiącu: {}, koszty całkowite miesiąca: {}, udział sprzedaży: {}, koszty miesiąca punktu: {}, dni ze sprzedażą punktu: {}, koszty dzienne punktu: {}",
                pointOfSaleName, from, averageDailyCostsForAllPoints, daysInMonth, totalMonthlyCosts, monthlySalesShare, pointOfSaleMonthlyCosts, daysWithSalesForPoint, pointOfSaleDailyCosts);
        
        List<DailyGrossMarginDto> dailyMargins = new ArrayList<>();

        for (DzienPodzial dzien : dailySales) {
            LocalDate dayDate = dzien.dzien();
            BigDecimal kitchenSales = dzien.kuchnia();
            BigDecimal buffetSales = dzien.bufet();
            BigDecimal packagingSales = dzien.opakowania();
            BigDecimal deliverySales = dzien.dowoz();
            BigDecimal totalSales = dzien.suma();

            // Oblicz koszty żywności (sprzedaż × food cost %)
            BigDecimal kitchenCost = kitchenSales.multiply(kitchenFoodCostPercent)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal buffetCost = buffetSales.multiply(buffetFoodCostPercent)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalCost = kitchenCost.add(buffetCost);

            // Marża brutto = sprzedaż - koszty żywności
            // Opakowania i dowóz mają 100% marży (brak kosztów żywności)
            BigDecimal grossMargin = totalSales.subtract(totalCost);

            // Koszty ogólne dla tego punktu sprzedaży - tylko dla dni ze sprzedażą
            // Dla dni bez sprzedaży koszty = 0
            BigDecimal costs = (totalSales.signum() == 0) 
                    ? BigDecimal.ZERO 
                    : pointOfSaleDailyCosts;

            // Marża netto = marża brutto - koszty ogólne
            BigDecimal netMargin = grossMargin.subtract(costs);

            dailyMargins.add(new DailyGrossMarginDto(
                    dayDate,
                    totalSales,
                    kitchenSales,
                    buffetSales,
                    packagingSales,
                    deliverySales,
                    kitchenCost,
                    buffetCost,
                    totalCost,
                    costs,
                    grossMargin,
                    netMargin,
                    netMargin.compareTo(BigDecimal.ZERO) > 0
            ));
        }

        // Podsumowanie - wyklucz dni z zerową sprzedażą
        List<DailyGrossMarginDto> daysWithSales = dailyMargins.stream()
                .filter(day -> day.totalSales().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        long profitDays = daysWithSales.stream().filter(DailyGrossMarginDto::isProfit).count();
        long lossDays = daysWithSales.size() - profitDays;

        BigDecimal totalSales = daysWithSales.stream()
                .map(DailyGrossMarginDto::totalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = daysWithSales.stream()
                .map(DailyGrossMarginDto::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCosts = daysWithSales.stream()
                .map(DailyGrossMarginDto::costs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGrossMargin = daysWithSales.stream()
                .map(DailyGrossMarginDto::grossMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetMargin = daysWithSales.stream()
                .map(DailyGrossMarginDto::netMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDailyMargin = daysWithSales.isEmpty()
                ? BigDecimal.ZERO
                : totalNetMargin.divide(BigDecimal.valueOf(daysWithSales.size()), 2, RoundingMode.HALF_UP);

        DailyGrossMarginDto bestDay = daysWithSales.stream()
                .max(Comparator.comparing(DailyGrossMarginDto::netMargin))
                .orElse(null);

        DailyGrossMarginDto worstDay = daysWithSales.stream()
                .min(Comparator.comparing(DailyGrossMarginDto::netMargin))
                .orElse(null);

        log.debug("Marża brutto/netto dzienna {} {} | sprzedawcy {} | dni z zyskiem: {} | dni ze stratą: {} | marża brutto: {} | marża netto: {}",
                pointOfSaleName, from, sellerIds, profitDays, lossDays, totalGrossMargin, totalNetMargin);

        return new DailyGrossMarginDto.MonthlySummary(
                from,
                to,
                pointOfSaleName,
                List.copyOf(sellerIds),
                kitchenFoodCost.foodCostPercent(),
                buffetFoodCost.foodCostPercent(),
                dailyMargins,
                profitDays,
                lossDays,
                totalSales,
                totalCost,
                totalCosts,
                totalGrossMargin,
                totalNetMargin,
                averageDailyMargin,
                bestDay,
                worstDay
        );
    }

    /**
     * Oblicza średnią dzienną kosztów dla danego roku.
     * Znajduje ostatni dzień ze sprzedażą w roku, sumuje koszty od początku roku do tego dnia,
     * i dzieli przez liczbę dni ZE SPRZEDAŻĄ (nie wszystkie dni od początku roku).
     * Koszty są zawsze obliczane dla WSZYSTKICH sprzedawców (wspólny magazyn).
     */
    private BigDecimal calculateAverageDailyCostsForYear(int year, Map<Integer, BigDecimal> yearlyAverageCostsCache) {
        // Sprawdź cache
        if (yearlyAverageCostsCache.containsKey(year)) {
            return yearlyAverageCostsCache.get(year);
        }
        
        // Zawsze używamy wszystkich sprzedawców (wspólny magazyn)
        Collection<Integer> allSellers = configService.getAllSellers();
        
        // Znajdź ostatni dzień ze sprzedażą w roku
        java.sql.Date lastSalesDateSql = rachunekRepository.findLastSalesDateInYear(year);
        
        if (lastSalesDateSql == null) {
            // Brak sprzedaży w roku - zwróć zero
            yearlyAverageCostsCache.put(year, BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }
        
        LocalDate lastSalesDate = lastSalesDateSql.toLocalDate();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        
        // Pobierz koszty od początku roku do ostatniego dnia ze sprzedażą
        FoodCostSummary costsSummary = calculateFoodCostForCosts(yearStart, lastSalesDate, allSellers);
        BigDecimal totalCosts = costsSummary.purchasesTotalNet();
        
        // Zlicz WSZYSTKIE dni kalendarzowe od początku roku do ostatniego dnia ze sprzedażą (włącznie)
        long totalCalendarDays = ChronoUnit.DAYS.between(yearStart, lastSalesDate) + 1;
        
        // Oblicz średnią dzienną - dzielimy przez wszystkie dni kalendarzowe (nie tylko dni ze sprzedażą)
        BigDecimal averageDailyCosts = (totalCalendarDays == 0 || totalCosts.signum() == 0)
                ? BigDecimal.ZERO
                : totalCosts.divide(BigDecimal.valueOf(totalCalendarDays), 2, RoundingMode.HALF_UP);
        
        // Zapisz w cache
        yearlyAverageCostsCache.put(year, averageDailyCosts);
        
        log.debug("Średnia dzienna kosztów dla roku {}: ostatni dzień ze sprzedażą: {}, wszystkie dni kalendarzowe: {}, koszty: {}, średnia: {}",
                year, lastSalesDate, totalCalendarDays, totalCosts, averageDailyCosts);
        
        return averageDailyCosts;
    }
    
    /**
     * Zwraca ostatni dzień ze sprzedażą w danym roku (dla wszystkich sprzedawców).
     * Metoda pomocnicza do sprawdzania informacji o ostatnim dniu sprzedaży.
     */
    @Transactional(readOnly = true)
    public LocalDate getLastSalesDateInYear(int year) {
        java.sql.Date lastSalesDateSql = rachunekRepository.findLastSalesDateInYear(year);
        return lastSalesDateSql != null ? lastSalesDateSql.toLocalDate() : null;
    }
    
    /**
     * Łączy dwa raporty marży brutto dziennej w jeden (sumuje dane z obu punktów sprzedaży).
     * Używane gdy wybieramy "Wszyscy" - sumujemy KD + Ratuszowa.
     */
    @Transactional(readOnly = true)
    public DailyGrossMarginDto.MonthlySummary combineDailyGrossMarginSummaries(
            DailyGrossMarginDto.MonthlySummary summary1,
            DailyGrossMarginDto.MonthlySummary summary2,
            String combinedPointOfSaleName) {
        
        // Sprawdź czy oba raporty są dla tego samego okresu
        if (!summary1.from().equals(summary2.from()) || !summary1.to().equals(summary2.to())) {
            throw new IllegalArgumentException("Raporty muszą być dla tego samego okresu");
        }
        
        // Stwórz mapę dni z pierwszego raportu
        Map<LocalDate, DailyGrossMarginDto> combinedDailyMargins = new HashMap<>();
        
        // Dodaj dane z pierwszego raportu
        for (DailyGrossMarginDto day : summary1.dailyMargins()) {
            combinedDailyMargins.put(day.date(), day);
        }
        
        // Dodaj/sumuj dane z drugiego raportu
        for (DailyGrossMarginDto day2 : summary2.dailyMargins()) {
            DailyGrossMarginDto day1 = combinedDailyMargins.get(day2.date());
            
            if (day1 != null) {
                // Sumuj dane dla tego samego dnia
                DailyGrossMarginDto combined = new DailyGrossMarginDto(
                        day1.date(),
                        day1.totalSales().add(day2.totalSales()),
                        day1.kitchenSales().add(day2.kitchenSales()),
                        day1.buffetSales().add(day2.buffetSales()),
                        day1.packagingSales().add(day2.packagingSales()),
                        day1.deliverySales().add(day2.deliverySales()),
                        day1.kitchenCost().add(day2.kitchenCost()),
                        day1.buffetCost().add(day2.buffetCost()),
                        day1.totalCost().add(day2.totalCost()),
                        day1.costs().add(day2.costs()),
                        day1.grossMargin().add(day2.grossMargin()),
                        day1.netMargin().add(day2.netMargin()),
                        day1.netMargin().add(day2.netMargin()).compareTo(BigDecimal.ZERO) > 0
                );
                combinedDailyMargins.put(day2.date(), combined);
            } else {
                // Tylko w drugim raporcie - dodaj bez zmian
                combinedDailyMargins.put(day2.date(), day2);
            }
        }
        
        // Posortuj dni
        List<DailyGrossMarginDto> sortedDailyMargins = combinedDailyMargins.values().stream()
                .sorted(Comparator.comparing(DailyGrossMarginDto::date))
                .collect(Collectors.toList());
        
        // Oblicz podsumowanie
        List<DailyGrossMarginDto> daysWithSales = sortedDailyMargins.stream()
                .filter(day -> day.totalSales().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        long profitDays = daysWithSales.stream().filter(DailyGrossMarginDto::isProfit).count();
        long lossDays = daysWithSales.size() - profitDays;
        
        BigDecimal totalSales = daysWithSales.stream()
                .map(DailyGrossMarginDto::totalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = daysWithSales.stream()
                .map(DailyGrossMarginDto::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCosts = daysWithSales.stream()
                .map(DailyGrossMarginDto::costs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalGrossMargin = daysWithSales.stream()
                .map(DailyGrossMarginDto::grossMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalNetMargin = daysWithSales.stream()
                .map(DailyGrossMarginDto::netMargin)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageDailyMargin = daysWithSales.isEmpty()
                ? BigDecimal.ZERO
                : totalNetMargin.divide(BigDecimal.valueOf(daysWithSales.size()), 2, RoundingMode.HALF_UP);
        
        DailyGrossMarginDto bestDay = daysWithSales.stream()
                .max(Comparator.comparing(DailyGrossMarginDto::netMargin))
                .orElse(null);
        
        DailyGrossMarginDto worstDay = daysWithSales.stream()
                .min(Comparator.comparing(DailyGrossMarginDto::netMargin))
                .orElse(null);
        
        // Użyj food cost % z pierwszego raportu (powinny być takie same)
        return new DailyGrossMarginDto.MonthlySummary(
                summary1.from(),
                summary1.to(),
                combinedPointOfSaleName,
                List.copyOf(summary1.sellerIds()), // Możemy użyć połączonej listy, ale to nie jest krytyczne
                summary1.kitchenFoodCostPercent(),
                summary1.buffetFoodCostPercent(),
                sortedDailyMargins,
                profitDays,
                lossDays,
                totalSales,
                totalCost,
                totalCosts,
                totalGrossMargin,
                totalNetMargin,
                averageDailyMargin,
                bestDay,
                worstDay
        );
    }
    
    /**
     * Oblicza udział obrotów danego punktu sprzedaży w całkowitym obrocie za CAŁY MIESIĄC.
     * Zwraca wartość od 0 do 1 (0 = brak obrotów, 1 = wszystkie obroty).
     * Jeśli całkowity obrót = 0, zwraca 0.
     * Ta proporcja jest używana do podziału kosztów między punktami sprzedaży.
     */
    private BigDecimal calculateMonthlySalesShareForPointOfSale(LocalDate monthStart, LocalDate monthEnd,
                                                                 Collection<Integer> pointOfSaleSellerIds) {
        // Oblicz całkowity obrót miesiąca dla wszystkich sprzedawców
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDateTime monthEndDateTime = monthEnd.plusDays(1).atStartOfDay();
        BigDecimal totalSalesAll = rachunekRepository.sumaRazemBySellers(monthStartDateTime, monthEndDateTime, 
                configService.getAllSellers());
        
        // Oblicz obrót miesiąca dla tego punktu sprzedaży
        BigDecimal pointOfSaleSales = rachunekRepository.sumaRazemBySellers(monthStartDateTime, monthEndDateTime, 
                pointOfSaleSellerIds);
        
        // Jeśli brak obrotów lub obrót punktu = 0, zwróć 0
        if (totalSalesAll.signum() == 0 || pointOfSaleSales.signum() == 0) {
            return BigDecimal.ZERO;
        }
        
        // Udział = obrót punktu / całkowity obrót miesiąca
        BigDecimal share = pointOfSaleSales.divide(totalSalesAll, 4, RoundingMode.HALF_UP);
        
        log.debug("Udział sprzedaży punktu {} w miesiącu {}: obrót punktu: {}, obrót całkowity: {}, udział: {}",
                pointOfSaleSellerIds, monthStart, pointOfSaleSales, totalSalesAll, share);
        
        return share;
    }
}


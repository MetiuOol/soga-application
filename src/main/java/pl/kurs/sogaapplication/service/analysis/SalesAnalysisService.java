package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.models.DzienPodzial;
import pl.kurs.sogaapplication.models.SprzedazKuchniaBufetOkres;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.config.PointOfSaleService;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;
import pl.kurs.sogaapplication.service.validation.BillValidationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis do analizy sprzedaży
 * Odpowiada za generowanie raportów sprzedaży i analizę kuchnia vs bufet
 */
@Service
public class SalesAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalesAnalysisService.class);
    
    private final RachunekJpaRepository rachunekRepository;
    private final RestaurantConfigService configService;
    private final PointOfSaleService pointOfSaleService;
    private final BillValidationService billValidationService;
    
    public SalesAnalysisService(RachunekJpaRepository rachunekRepository, 
                               RestaurantConfigService configService,
                               PointOfSaleService pointOfSaleService,
                               BillValidationService billValidationService) {
        this.rachunekRepository = rachunekRepository;
        this.configService = configService;
        this.pointOfSaleService = pointOfSaleService;
        this.billValidationService = billValidationService;
    }
    
    /**
     * Generuje pełny raport sprzedaży dla danego okresu
     */
    @Transactional(readOnly = true)
    public RestaurantReportDto generateSalesReport(LocalDate from, LocalDate to) {
        return generateSalesReport(from, to, configService.getDefaultSellers());
    }
    
    /**
     * Generuje pełny raport sprzedaży dla danego okresu i sprzedawców
     */
    @Transactional(readOnly = true)
    public RestaurantReportDto generateSalesReport(LocalDate from, LocalDate to, List<Integer> sellerIds) {
        logger.info("Generowanie raportu sprzedaży od {} do {} dla sprzedawców: {}", 
                from, to, sellerIds);
        
        var kitchenProducts = configService.getKitchenProducts();
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();
        
        // Analiza kuchnia / bufet / opakowania / dowóz dla całego okresu
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);
        
        SprzedazKuchniaBufetOkres kitchenBuffetTotal = analyzeKitchenBuffetSales(
                fromDateTime, toDateTime, kitchenProducts, packagingProducts, deliveryProducts);
        
        RestaurantReportDto.KitchenBuffetSales kitchenBuffetSales =
                new RestaurantReportDto.KitchenBuffetSales(
                        kitchenBuffetTotal.kuchniaNetto(),
                        kitchenBuffetTotal.bufetNetto(),
                        kitchenBuffetTotal.opakowaniaNetto(),
                        kitchenBuffetTotal.dowozNetto(),
                        kitchenBuffetTotal.sumaRazem()
                );
        
        // Analiza dzienna dla pełnego zakresu dat [from, to]
        List<DzienPodzial> dailyResults = analyzeDailySalesForRange(from, to, sellerIds, kitchenProducts);
        
        List<RestaurantReportDto.DailySales> dailySales = dailyResults.stream()
                .map(d -> new RestaurantReportDto.DailySales(
                        d.dzien(),
                        d.kuchnia(),
                        d.bufet(),
                        d.opakowania(),
                        d.dowoz(),
                        d.suma()))
                .collect(Collectors.toList());
        
        // Walidacja podejrzanych rachunków
        var suspiciousBills = billValidationService.findSuspiciousBills(from, to);
        var suspiciousStats = billValidationService.getStats(suspiciousBills);
        
        RestaurantReportDto report = RestaurantReportDto.create(
                from, to, sellerIds, kitchenBuffetSales, dailySales, 
                suspiciousBills, suspiciousStats);
        
        logSalesReportSummary(report);
        
        return report;
    }
    
    /**
     * Analizuje sprzedaż kuchnia vs bufet dla danego okresu
     */
    @Transactional(readOnly = true)
    public SprzedazKuchniaBufetOkres analyzeKitchenBuffetSales(LocalDateTime from, LocalDateTime to,
                                                               Collection<Long> kitchenProductIds,
                                                               Collection<Long> packagingProductIds,
                                                               Collection<Long> deliveryProductIds) {
        logger.debug("Analiza sprzedaży kuchnia vs bufet od {} do {}", from, to);
        
        var totalSales = rachunekRepository.sumaRazem(from, to);
        var kitchenSales = sumByProducts(from, to, kitchenProductIds);
        var packagingSales = sumByProducts(from, to, packagingProductIds);
        var deliverySales = sumByProducts(from, to, deliveryProductIds);
        var buffetSales = totalSales
                .subtract(kitchenSales)
                .subtract(packagingSales)
                .subtract(deliverySales);

        return new SprzedazKuchniaBufetOkres(kitchenSales, buffetSales, packagingSales, deliverySales, totalSales);
    }
    
    /**
     * Analizuje sprzedaż dzienną dla danego miesiąca (publiczna metoda, zachowana dla eksportu miesięcznego).
     * Używa tylko pierwszego dnia miesiąca i analizuje pełny miesiąc.
     */
    @Transactional(readOnly = true)
    public List<DzienPodzial> analyzeDailySales(LocalDate firstDayOfMonth, Collection<Integer> sellerIds) {
        return analyzeDailySalesForMonth(firstDayOfMonth, sellerIds, configService.getKitchenProducts());
    }
    
    /**
     * Analizuje sprzedaż dzienną dla danego miesiąca (prywatna metoda, używana do eksportu miesięcznego).
     * Zakres: od pierwszego dnia miesiąca do ostatniego dnia miesiąca.
     */
    @Transactional(readOnly = true)
    private List<DzienPodzial> analyzeDailySalesForMonth(LocalDate firstDayOfMonth,
                                                        Collection<Integer> sellerIds,
                                                        Collection<Long> kitchenProductIds) {
        logger.debug("Analiza sprzedaży dziennej dla miesiąca {} dla sprzedawców: {}", 
                firstDayOfMonth, sellerIds);
        
        var lastDay = firstDayOfMonth.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        var results = new java.util.ArrayList<DzienPodzial>();
        
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();

        for (var date = firstDayOfMonth; !date.isAfter(lastDay); date = date.plusDays(1)) {
            var from = date.atStartOfDay();
            var to = date.plusDays(1).atStartOfDay();
            
            var totalSales = rachunekRepository.sumaRazemBySellers(from, to, sellerIds);
            var kitchenSales = (kitchenProductIds == null || kitchenProductIds.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, kitchenProductIds, sellerIds);
            var packagingSales = (packagingProducts == null || packagingProducts.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, packagingProducts, sellerIds);
            var deliverySales = (deliveryProducts == null || deliveryProducts.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, deliveryProducts, sellerIds);
            var buffetSales = totalSales
                    .subtract(kitchenSales)
                    .subtract(packagingSales)
                    .subtract(deliverySales);
            
            results.add(new DzienPodzial(date, kitchenSales, buffetSales, packagingSales, deliverySales, totalSales));
        }
        
        return results;
    }

    /**
     * Analiza dzienna dla dowolnego zakresu dat [from, to] włącznie.
     */
    @Transactional(readOnly = true)
    private List<DzienPodzial> analyzeDailySalesForRange(LocalDate fromDate,
                                                         LocalDate toDate,
                                                         Collection<Integer> sellerIds,
                                                         Collection<Long> kitchenProductIds) {
        logger.debug("Analiza sprzedaży dziennej dla zakresu {} - {} dla sprzedawców: {}",
                fromDate, toDate, sellerIds);

        var results = new java.util.ArrayList<DzienPodzial>();

        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();

        for (var date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            var from = date.atStartOfDay();
            var to = date.plusDays(1).atStartOfDay();

            var totalSales = rachunekRepository.sumaRazemBySellers(from, to, sellerIds);
            var kitchenSales = (kitchenProductIds == null || kitchenProductIds.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, kitchenProductIds, sellerIds);
            var packagingSales = (packagingProducts == null || packagingProducts.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, packagingProducts, sellerIds);
            var deliverySales = (deliveryProducts == null || deliveryProducts.isEmpty())
                    ? java.math.BigDecimal.ZERO
                    : rachunekRepository.sumaKuchniaBySellers(from, to, deliveryProducts, sellerIds);
            var buffetSales = totalSales
                    .subtract(kitchenSales)
                    .subtract(packagingSales)
                    .subtract(deliverySales);

            results.add(new DzienPodzial(date, kitchenSales, buffetSales, packagingSales, deliverySales, totalSales));
        }

        return results;
    }
    
    /**
     * Analizuje sprzedaż dla konkretnego dnia
     */
    @Transactional(readOnly = true)
    public SprzedazKuchniaBufetOkres analyzeDailyKitchenBuffet(LocalDate date, Collection<Integer> sellerIds) {
        var from = date.atStartOfDay();
        var to = date.plusDays(1).atStartOfDay();
        var kitchenProducts = configService.getKitchenProducts();
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();

        var totalSales = rachunekRepository.sumaRazemBySellers(from, to, sellerIds);
        var kitchenSales = (kitchenProducts == null || kitchenProducts.isEmpty())
                ? java.math.BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(from, to, kitchenProducts, sellerIds);
        var packagingSales = (packagingProducts == null || packagingProducts.isEmpty())
                ? java.math.BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(from, to, packagingProducts, sellerIds);
        var deliverySales = (deliveryProducts == null || deliveryProducts.isEmpty())
                ? java.math.BigDecimal.ZERO
                : rachunekRepository.sumaKuchniaBySellers(from, to, deliveryProducts, sellerIds);
        var buffetSales = totalSales
                .subtract(kitchenSales)
                .subtract(packagingSales)
                .subtract(deliverySales);
        
        return new SprzedazKuchniaBufetOkres(kitchenSales, buffetSales, packagingSales, deliverySales, totalSales);
    }

    private java.math.BigDecimal sumByProducts(LocalDateTime from, LocalDateTime to, Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return rachunekRepository.sumaKuchnia(from, to, productIds);
    }
    
    private void logSalesReportSummary(RestaurantReportDto report) {
        logger.info("=== PODSUMOWANIE RAPORTU SPRZEDAŻY ===");
        logger.info("Okres: {} - {}", report.from(), report.to());
        logger.info("Sprzedawcy: {}", report.sellerIds());
        logger.info("Sprzedaż kuchnia: {}", report.kitchenSales());
        logger.info("Sprzedaż bufet: {}", report.buffetSales());
        logger.info("Sprzedaż łącznie: {}", report.totalSales());
        logger.info("Liczba dni z danymi: {}", report.dailySales().size());
        
        // Znajdź najlepszy i najgorszy dzień
        var bestDay = report.dailySales().stream()
                .max((a, b) -> a.total().compareTo(b.total()))
                .orElse(null);
        var worstDay = report.dailySales().stream()
                .min((a, b) -> a.total().compareTo(b.total()))
                .orElse(null);
        
        if (bestDay != null && worstDay != null) {
            logger.info("Najlepszy dzień: {} - {}", bestDay.date(), bestDay.total());
            logger.info("Najgorszy dzień: {} - {}", worstDay.date(), worstDay.total());
        }
        logger.info("=====================================");
    }
}

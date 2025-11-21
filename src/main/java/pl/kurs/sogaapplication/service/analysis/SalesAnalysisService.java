package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.RestaurantReportDto;
import pl.kurs.sogaapplication.dto.SalesItemDetailDto;
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
        
        // Analiza dzienna dla pełnego zakresu dat [from, to]
        // Używamy tylko getDailySalesDetails, żeby zapewnić spójność
        List<DzienPodzial> dailyResults = analyzeDailySalesForRange(from, to, sellerIds, kitchenProducts);
        
        // Oblicz sumy całkowite z danych dziennych (używamy tej samej metody co raport dzienny)
        java.math.BigDecimal totalKitchen = dailyResults.stream()
                .map(DzienPodzial::kuchnia)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalBuffet = dailyResults.stream()
                .map(DzienPodzial::bufet)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalPackaging = dailyResults.stream()
                .map(DzienPodzial::opakowania)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalDelivery = dailyResults.stream()
                .map(DzienPodzial::dowoz)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalSales = dailyResults.stream()
                .map(DzienPodzial::suma)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        RestaurantReportDto.KitchenBuffetSales kitchenBuffetSales =
                new RestaurantReportDto.KitchenBuffetSales(
                        totalKitchen,
                        totalBuffet,
                        totalPackaging,
                        totalDelivery,
                        totalSales
                );
        
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
     * Analizuje sprzedaż kuchnia vs bufet dla danego okresu.
     * Bufet jest liczony explicite (produkty bufetowe + grupy bufetowe), reszta to kuchnia.
     */
    @Transactional(readOnly = true)
    public SprzedazKuchniaBufetOkres analyzeKitchenBuffetSales(LocalDateTime from, LocalDateTime to,
                                                               Collection<Long> kitchenProductIds,
                                                               Collection<Long> packagingProductIds,
                                                               Collection<Long> deliveryProductIds) {
        logger.debug("Analiza sprzedaży kuchnia vs bufet od {} do {}", from, to);
        
        var totalSales = rachunekRepository.sumaRazem(from, to);
        var packagingSales = sumByProducts(from, to, packagingProductIds);
        var deliverySales = sumByProducts(from, to, deliveryProductIds);
        
        // Oblicz bufet explicite (produkty bufetowe + grupy bufetowe)
        var buffetProducts = configService.getBuffetProducts();
        var buffetGroups = configService.getBuffetGroups();
        
        java.math.BigDecimal buffetSalesByProducts = (buffetProducts == null || buffetProducts.isEmpty())
                ? java.math.BigDecimal.ZERO
                : sumByProducts(from, to, buffetProducts);
        
        java.math.BigDecimal buffetSalesByGroups = (buffetGroups == null || buffetGroups.isEmpty())
                ? java.math.BigDecimal.ZERO
                : sumBufetByGroups(from, to, buffetGroups);
        
        // Bufet = suma produktów bufetowych + suma grup bufetowych (może się nakładać, więc weź maksimum)
        var buffetSales = buffetSalesByProducts.add(buffetSalesByGroups);
        
        // Kuchnia = Całkowita - Bufet - Opakowania - Dowóz
        var kitchenSales = totalSales
                .subtract(buffetSales)
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
     * Używa tej samej logiki kategoryzacji co getDailySalesDetails, żeby zapewnić spójność.
     */
    @Transactional(readOnly = true)
    private List<DzienPodzial> analyzeDailySalesForRange(LocalDate fromDate,
                                                         LocalDate toDate,
                                                         Collection<Integer> sellerIds,
                                                         Collection<Long> kitchenProductIds) {
        logger.debug("Analiza sprzedaży dziennej dla zakresu {} - {} dla sprzedawców: {}",
                fromDate, toDate, sellerIds);

        var results = new java.util.ArrayList<DzienPodzial>();

        for (var date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            // Użyj tej samej metody co szczegóły, żeby zapewnić spójność
            var details = getDailySalesDetails(date, sellerIds, "");
            
            // Oblicz sumy z kategoryzowanych pozycji
            java.math.BigDecimal kitchenSales = details.stream()
                    .filter(item -> "kitchen".equals(item.category()))
                    .map(SalesItemDetailDto::wartoscNetto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            java.math.BigDecimal buffetSales = details.stream()
                    .filter(item -> "buffet".equals(item.category()))
                    .map(SalesItemDetailDto::wartoscNetto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            java.math.BigDecimal packagingSales = details.stream()
                    .filter(item -> "packaging".equals(item.category()))
                    .map(SalesItemDetailDto::wartoscNetto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            java.math.BigDecimal deliverySales = details.stream()
                    .filter(item -> "delivery".equals(item.category()))
                    .map(SalesItemDetailDto::wartoscNetto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
            java.math.BigDecimal totalSales = details.stream()
                    .map(SalesItemDetailDto::wartoscNetto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            results.add(new DzienPodzial(date, kitchenSales, buffetSales, packagingSales, deliverySales, totalSales));
        }

        return results;
    }
    
    /**
     * Analizuje sprzedaż dla konkretnego dnia.
     * Używa tej samej logiki kategoryzacji co getDailySalesDetails, żeby zapewnić spójność.
     */
    @Transactional(readOnly = true)
    public SprzedazKuchniaBufetOkres analyzeDailyKitchenBuffet(LocalDate date, Collection<Integer> sellerIds) {
        // Użyj tej samej metody co szczegóły, żeby zapewnić spójność
        var details = getDailySalesDetails(date, sellerIds, "");
        
        // Oblicz sumy z kategoryzowanych pozycji
        java.math.BigDecimal kitchenSales = details.stream()
                .filter(item -> "kitchen".equals(item.category()))
                .map(SalesItemDetailDto::wartoscNetto)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal buffetSales = details.stream()
                .filter(item -> "buffet".equals(item.category()))
                .map(SalesItemDetailDto::wartoscNetto)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal packagingSales = details.stream()
                .filter(item -> "packaging".equals(item.category()))
                .map(SalesItemDetailDto::wartoscNetto)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal deliverySales = details.stream()
                .filter(item -> "delivery".equals(item.category()))
                .map(SalesItemDetailDto::wartoscNetto)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        java.math.BigDecimal totalSales = details.stream()
                .map(SalesItemDetailDto::wartoscNetto)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        return new SprzedazKuchniaBufetOkres(kitchenSales, buffetSales, packagingSales, deliverySales, totalSales);
    }

    private java.math.BigDecimal sumByProducts(LocalDateTime from, LocalDateTime to, Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return rachunekRepository.sumaKuchnia(from, to, productIds);
    }

    /**
     * Sumuje sprzedaż bufetu po grupach bufetowych (ID_GR) bez filtrowania po sprzedawcach.
     * Używane w analyzeKitchenBuffetSales, która nie przyjmuje sellerIds.
     * Wyklucza produkty już w buffetProducts, żeby uniknąć podwójnego liczenia.
     */
    private java.math.BigDecimal sumBufetByGroups(LocalDateTime from, LocalDateTime to, Collection<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        var buffetProducts = configService.getBuffetProducts();
        var excludedProducts = (buffetProducts == null || buffetProducts.isEmpty()) ? java.util.Collections.<Long>emptyList() : buffetProducts;
        return rachunekRepository.sumaBufetByGroups(from, to, groupIds, excludedProducts, excludedProducts.size());
    }
    
    /**
     * Pobiera szczegółową listę pozycji sprzedanych dla konkretnej daty i sprzedawców.
     * Kategoryzuje pozycje na kuchnię/bufet/opakowania/dowóz na podstawie ID_TW.
     */
    @Transactional(readOnly = true)
    public List<SalesItemDetailDto> getDailySalesDetails(LocalDate date, Collection<Integer> sellerIds, String pointOfSaleName) {
        logger.debug("Pobieranie szczegółów sprzedaży dla dnia {} dla sprzedawców: {}", date, sellerIds);

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        var kitchenProducts = configService.getKitchenProducts();
        var buffetProducts = configService.getBuffetProducts();
        var buffetGroups = configService.getBuffetGroups();
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();

        List<Object[]> rawItems = rachunekRepository.findSalesItemsByDateAndSellers(from, to, sellerIds);

        List<SalesItemDetailDto> items = new java.util.ArrayList<>();

        for (Object[] row : rawItems) {
            Long rachunekId = ((Number) row[0]).longValue();
            String sellerName = (String) row[1];
            Integer sellerId = ((Number) row[2]).intValue();
            Long towarId = row[3] != null ? ((Number) row[3]).longValue() : null;
            String towarNazwa = row[4] != null ? (String) row[4] : "";
            Integer towarGrupa = row[5] != null ? ((Number) row[5]).intValue() : null;
            java.math.BigDecimal ilosc = (java.math.BigDecimal) row[6];
            java.math.BigDecimal wartoscNetto = (java.math.BigDecimal) row[7];

            // Określ kategorię na podstawie ID_TW i ID_GR - sztywny podział, bez domyślnych opcji
            String category = "undefined"; // domyślnie niezdefiniowane
            if (towarId != null) {
                // Sprawdź explicite zdefiniowane kategorie (kolejność ma znaczenie)
                if (packagingProducts.contains(towarId)) {
                    category = "packaging";
                } else if (deliveryProducts.contains(towarId)) {
                    category = "delivery";
                } else if (kitchenProducts.contains(towarId)) {
                    category = "kitchen";
                } else if (buffetProducts.contains(towarId)) {
                    category = "buffet";
                } else if (towarGrupa != null && buffetGroups.contains(towarGrupa)) {
                    category = "buffet";
                }
                // Jeśli nie pasuje do żadnej kategorii, zostaje "undefined"
            }

            items.add(new SalesItemDetailDto(
                    rachunekId,
                    sellerName,
                    sellerId,
                    towarId,
                    towarNazwa,
                    towarGrupa,
                    ilosc,
                    wartoscNetto,
                    category
            ));
        }

        logger.debug("Znaleziono {} pozycji dla dnia {}", items.size(), date);
        return items;
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

package pl.kurs.sogaapplication.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.KitchenPurchasesSummary;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Abstrakcyjna klasa bazowa dla kalkulatorów food cost różnych magazynów.
 * Zawiera wspólną logikę dla wszystkich magazynów.
 * 
 * @param <T> Typ magazynu (może być użyteczny w przyszłości, obecnie nie jest używany)
 */
public abstract class AbstractWarehouseFoodCostCalculator implements WarehouseFoodCostCalculator {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final org.springframework.beans.factory.ObjectProvider<FoodCostService> foodCostServiceProvider;
    protected final RestaurantConfigService configService;
    
    protected AbstractWarehouseFoodCostCalculator(
            org.springframework.beans.factory.ObjectProvider<FoodCostService> foodCostServiceProvider,
            RestaurantConfigService configService) {
        this.foodCostServiceProvider = foodCostServiceProvider;
        this.configService = configService;
    }
    
    /**
     * Pobiera FoodCostService z ObjectProvider (rozwiązuje cykliczną zależność).
     */
    protected FoodCostService getFoodCostService() {
        return foodCostServiceProvider.getObject();
    }
    
    /**
     * Pobiera listę ID magazynów dla danego typu magazynu.
     * Musi być zaimplementowane w klasach potomnych.
     */
    protected abstract List<Integer> getWarehouseIds();
    
    /**
     * Oblicza sprzedaż dla danego magazynu w zadanym okresie.
     * Różni się dla każdego typu magazynu (kuchnia, bufet, koszty).
     * 
     * @param from Data początkowa
     * @param to Data końcowa
     * @param sellerIds Lista ID sprzedawców
     * @return Wartość sprzedaży netto
     */
    protected abstract BigDecimal calculateSales(LocalDate from, LocalDate to, Collection<Integer> sellerIds);
    
    @Override
    public FoodCostSummary calculateFoodCost(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var warehouseIds = getWarehouseIds();
        if (warehouseIds.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych magazynów: " + getWarehouseName());
        }
        
        // Pobierz zakupy (wspólna logika dla wszystkich magazynów)
        var purchasesSummary = getFoodCostService().calculateWarehousePurchases(from, to, warehouseIds, getWarehouseName());
        
        // Oblicz sprzedaż (specyficzne dla każdego magazynu)
        BigDecimal sales = calculateSales(from, to, sellerIds);
        
        // Oblicz food cost % (wspólna logika)
        BigDecimal foodCostPercent = sales.signum() == 0
                ? BigDecimal.ZERO
                : purchasesSummary.purchasesTotalNet()
                .divide(sales, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Food cost {} {} - {} | sprzedawcy {} | magazyny {} | sprzedaż {} | zakupy {} | food cost {}%",
                getWarehouseName(), from, to, sellerIds, warehouseIds, sales, purchasesSummary.purchasesTotalNet(), foodCostPercent);
        
        return new FoodCostSummary(
                from,
                to,
                List.copyOf(sellerIds),
                List.copyOf(warehouseIds),
                sales,
                purchasesSummary.purchasesFzNet(),
                purchasesSummary.purchasesPzNet(),
                purchasesSummary.purchasesTotalNet(),
                foodCostPercent
        );
    }
    
    @Override
    public KitchenPurchasesSummary calculatePurchases(LocalDate from, LocalDate to) {
        var warehouseIds = getWarehouseIds();
        return getFoodCostService().calculateWarehousePurchases(from, to, warehouseIds, getWarehouseName());
    }
}




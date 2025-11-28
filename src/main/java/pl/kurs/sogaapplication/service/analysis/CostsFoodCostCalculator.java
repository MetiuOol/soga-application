package pl.kurs.sogaapplication.service.analysis;

import org.springframework.stereotype.Component;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Kalkulator food cost dla magazynu Koszty.
 * Magazyn koszty nie ma sprzedaży, więc nie można obliczyć food cost %.
 * Zwraca tylko podsumowanie zakupów.
 */
@Component
public class CostsFoodCostCalculator extends AbstractWarehouseFoodCostCalculator {
    
    public CostsFoodCostCalculator(org.springframework.beans.factory.ObjectProvider<FoodCostService> foodCostServiceProvider,
                                   RestaurantConfigService configService) {
        super(foodCostServiceProvider, configService);
    }
    
    @Override
    public String getWarehouseName() {
        return "Koszty";
    }
    
    @Override
    protected List<Integer> getWarehouseIds() {
        return configService.getCostWarehouses();
    }
    
    @Override
    protected BigDecimal calculateSales(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        // Magazyn koszty nie ma sprzedaży
        return BigDecimal.ZERO;
    }
    
    @Override
    public boolean hasSales() {
        return false; // Magazyn koszty nie ma sprzedaży
    }
}


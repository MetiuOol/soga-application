package pl.kurs.sogaapplication.service.analysis;

import org.springframework.stereotype.Component;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.config.RestaurantConfigService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Kalkulator food cost dla magazynu Kuchnia.
 */
@Component
public class KitchenFoodCostCalculator extends AbstractWarehouseFoodCostCalculator {
    
    private final RachunekJpaRepository rachunekRepository;
    
    public KitchenFoodCostCalculator(org.springframework.beans.factory.ObjectProvider<FoodCostService> foodCostServiceProvider,
                                    RestaurantConfigService configService,
                                    RachunekJpaRepository rachunekRepository) {
        super(foodCostServiceProvider, configService);
        this.rachunekRepository = rachunekRepository;
    }
    
    @Override
    public String getWarehouseName() {
        return "Kuchnia";
    }
    
    @Override
    protected List<Integer> getWarehouseIds() {
        return configService.getKitchenWarehouses();
    }
    
    @Override
    protected BigDecimal calculateSales(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var kitchenProducts = configService.getKitchenProducts();
        if (kitchenProducts.isEmpty()) {
            throw new IllegalStateException("Brak skonfigurowanych produktów kuchni (restaurant.kitchen.products)");
        }
        
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
        
        return rachunekRepository.sumaKuchniaBySellers(
                fromDateTime,
                toDateTime,
                kitchenProducts,
                sellerIds
        );
    }
    
    @Override
    public boolean hasSales() {
        return true;
    }
}


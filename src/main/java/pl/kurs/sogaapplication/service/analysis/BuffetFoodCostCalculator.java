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
 * Kalkulator food cost dla magazynu Bufet.
 */
@Component
public class BuffetFoodCostCalculator extends AbstractWarehouseFoodCostCalculator {
    
    private final RachunekJpaRepository rachunekRepository;
    
    public BuffetFoodCostCalculator(org.springframework.beans.factory.ObjectProvider<FoodCostService> foodCostServiceProvider,
                                   RestaurantConfigService configService,
                                   RachunekJpaRepository rachunekRepository) {
        super(foodCostServiceProvider, configService);
        this.rachunekRepository = rachunekRepository;
    }
    
    @Override
    public String getWarehouseName() {
        return "Bufet";
    }
    
    @Override
    protected List<Integer> getWarehouseIds() {
        return configService.getBuffetWarehouses();
    }
    
    @Override
    protected BigDecimal calculateSales(LocalDate from, LocalDate to, Collection<Integer> sellerIds) {
        var kitchenProducts = configService.getKitchenProducts();
        var packagingProducts = configService.getPackagingProducts();
        var deliveryProducts = configService.getDeliveryProducts();
        
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
        
        // Sprzedaż bufetu = całkowita sprzedaż - sprzedaż kuchni - sprzedaż opakowań - sprzedaż dowozu
        return totalSales
                .subtract(kitchenSales)
                .subtract(packagingSales)
                .subtract(deliverySales);
    }
    
    @Override
    public boolean hasSales() {
        return true;
    }
}


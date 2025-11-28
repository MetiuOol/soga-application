package pl.kurs.sogaapplication.service.analysis;

import pl.kurs.sogaapplication.dto.FoodCostSummary;
import pl.kurs.sogaapplication.dto.KitchenPurchasesSummary;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Interfejs dla kalkulatorów food cost dla różnych magazynów.
 * Każdy magazyn (Kuchnia, Bufet, Koszty) ma swoją implementację tego interfejsu.
 */
public interface WarehouseFoodCostCalculator {
    
    /**
     * Nazwa magazynu (np. "Kuchnia", "Bufet", "Koszty").
     */
    String getWarehouseName();
    
    /**
     * Oblicza food cost dla magazynu: porównuje zakupy ze sprzedażą w danym okresie.
     * 
     * @param from Data początkowa (włącznie)
     * @param to Data końcowa (włącznie)
     * @param sellerIds Lista ID sprzedawców
     * @return FoodCostSummary z zakupami, sprzedażą i food cost %
     */
    FoodCostSummary calculateFoodCost(LocalDate from, LocalDate to, Collection<Integer> sellerIds);
    
    /**
     * Oblicza tylko podsumowanie zakupów (bez porównania ze sprzedażą).
     * Przydatne dla magazynów bez sprzedaży (np. Koszty).
     * 
     * @param from Data początkowa (włącznie)
     * @param to Data końcowa (włącznie)
     * @return KitchenPurchasesSummary z zakupami
     */
    KitchenPurchasesSummary calculatePurchases(LocalDate from, LocalDate to);
    
    /**
     * Sprawdza czy magazyn ma sprzedaż (czyli czy można obliczyć food cost %).
     * Dla magazynu Koszty zwraca false.
     */
    boolean hasSales();
}




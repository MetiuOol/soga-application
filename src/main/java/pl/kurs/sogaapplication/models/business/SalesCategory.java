package pl.kurs.sogaapplication.models.business;

/**
 * Kategorie sprzedaży w punktach sprzedaży
 * Określa różne typy sprzedaży dostępne w danym punkcie
 */
public enum SalesCategory {
    /**
     * Sprzedaż w restauracji - kuchnia (posiłki gotowane)
     */
    RESTAURANT_KITCHEN,
    
    /**
     * Sprzedaż w restauracji - bufet (napoje, przekąski)
     */
    RESTAURANT_BUFFET,
    
    /**
     * Sprzedaż na wagę - jedzenie na wagę
     */
    WEIGHT_BASED,
    
    /**
     * Sprzedaż zup
     */
    SOUPS,
    
    /**
     * Abonamenty miesięczne
     */
    SUBSCRIPTIONS,
    
    /**
     * Karnety zwykłe
     */
    REGULAR_VOUCHERS,
    
    /**
     * Karnety mięsne
     */
    MEAT_VOUCHERS,
    
    /**
     * Opakowania na wynos
     */
    TAKEAWAY_PACKAGING;
    
    /**
     * Sprawdza czy kategoria należy do restauracji
     */
    public boolean isRestaurant() {
        return this == RESTAURANT_KITCHEN || this == RESTAURANT_BUFFET;
    }
    
    /**
     * Sprawdza czy kategoria należy do kuchni domowej
     */
    public boolean isKuchniaDomowa() {
        return this == WEIGHT_BASED || this == SOUPS || 
               this == SUBSCRIPTIONS || this == REGULAR_VOUCHERS || 
               this == MEAT_VOUCHERS || this == TAKEAWAY_PACKAGING;
    }
}






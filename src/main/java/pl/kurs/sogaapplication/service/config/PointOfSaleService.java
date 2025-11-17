package pl.kurs.sogaapplication.service.config;

import org.springframework.stereotype.Service;
import pl.kurs.sogaapplication.models.business.PointOfSale;
import pl.kurs.sogaapplication.models.business.SalesCategory;
import pl.kurs.sogaapplication.models.business.WorkingHours;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serwis zarządzający punktami sprzedaży w biznesie
 * Definiuje charakterystyki każdego punktu: godziny pracy, sprzedawców, kategorie sprzedaży
 */
@Service
public class PointOfSaleService {
    
    private final Map<String, PointOfSale> pointsOfSale;
    
    public PointOfSaleService(RestaurantConfigService configService) {
        this.pointsOfSale = initializePointsOfSale(configService);
    }
    
    /**
     * Inicjalizuje punkty sprzedaży z konfiguracją
     */
    private Map<String, PointOfSale> initializePointsOfSale(RestaurantConfigService configService) {
        Map<String, PointOfSale> points = new HashMap<>();
        
        // KUCHNIA DOMOWA
        // Godziny: 11-18 pn-pt, 11-14 sobota
        WorkingHours kdHours = new WorkingHours(Map.of(
                DayOfWeek.MONDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(18, 0)),
                DayOfWeek.TUESDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(18, 0)),
                DayOfWeek.WEDNESDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(18, 0)),
                DayOfWeek.THURSDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(18, 0)),
                DayOfWeek.FRIDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(18, 0)),
                DayOfWeek.SATURDAY, new WorkingHours.TimeRange(LocalTime.of(11, 0), LocalTime.of(14, 0))
                // Niedziela zamknięta - nie dodajemy do mapy
        ));
        
        PointOfSale kuchniaDomowa = new PointOfSale(
                "KD",
                "Kuchnia Domowa",
                11, // ID użytkownika dla KD
                List.of(11), // Lista sprzedawców dla KD
                kdHours,
                Set.of(
                        SalesCategory.WEIGHT_BASED,
                        SalesCategory.SOUPS,
                        SalesCategory.SUBSCRIPTIONS,
                        SalesCategory.REGULAR_VOUCHERS,
                        SalesCategory.MEAT_VOUCHERS,
                        SalesCategory.TAKEAWAY_PACKAGING
                )
        );
        points.put("KD", kuchniaDomowa);
        
        // RATUSZOWA
        // Godziny: 12-21 codziennie (czasem dłużej, niektóre święta zamknięte)
        WorkingHours ratuszowaHours = new WorkingHours(Map.of(
                DayOfWeek.MONDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.TUESDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.WEDNESDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.THURSDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.FRIDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.SATURDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0)),
                DayOfWeek.SUNDAY, new WorkingHours.TimeRange(LocalTime.of(12, 0), LocalTime.of(21, 0))
        ));
        
        // Wszyscy sprzedawcy oprócz ID 11 (KD)
        List<Integer> allSellers = configService.getAllSellers();
        List<Integer> ratuszowaSellers = allSellers.stream()
                .filter(id -> !id.equals(11))
                .collect(Collectors.toList());
        
        PointOfSale ratuszowa = new PointOfSale(
                "RATUSZOWA",
                "Ratuszowa",
                null, // Brak konkretnego ID użytkownika - wielu sprzedawców
                ratuszowaSellers,
                ratuszowaHours,
                Set.of(
                        SalesCategory.RESTAURANT_KITCHEN,
                        SalesCategory.RESTAURANT_BUFFET
                )
        );
        points.put("RATUSZOWA", ratuszowa);
        
        return Collections.unmodifiableMap(points);
    }
    
    /**
     * Zwraca punkt sprzedaży po ID
     */
    public Optional<PointOfSale> getPointOfSale(String id) {
        return Optional.ofNullable(pointsOfSale.get(id));
    }
    
    /**
     * Zwraca punkt sprzedaży dla danego ID użytkownika (sprzedawcy)
     */
    public Optional<PointOfSale> getPointOfSaleBySellerId(Integer sellerId) {
        return pointsOfSale.values().stream()
                .filter(pos -> pos.hasSeller(sellerId))
                .findFirst();
    }
    
    /**
     * Zwraca wszystkie punkty sprzedaży
     */
    public Collection<PointOfSale> getAllPointsOfSale() {
        return pointsOfSale.values();
    }
    
    /**
     * Zwraca listę ID punktów sprzedaży
     */
    public Set<String> getPointOfSaleIds() {
        return pointsOfSale.keySet();
    }
    
    /**
     * Sprawdza czy dany sprzedawca należy do określonego punktu sprzedaży
     */
    public boolean isSellerInPointOfSale(Integer sellerId, String pointOfSaleId) {
        return getPointOfSale(pointOfSaleId)
                .map(pos -> pos.hasSeller(sellerId))
                .orElse(false);
    }
}






package pl.kurs.sogaapplication.models.business;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Model reprezentujący punkt sprzedaży w biznesie
 * Każdy punkt sprzedaży ma swoje charakterystyki: godziny pracy, sprzedawców, typy sprzedaży
 */
public class PointOfSale {
    
    private final String id;
    private final String nazwa;
    private final Integer idUzytkownika; // ID sprzedawcy przypisanego do tego punktu (jeśli jest)
    private final List<Integer> sellerIds; // Lista ID wszystkich sprzedawców przypisanych do tego punktu
    private final WorkingHours workingHours;
    private final Set<SalesCategory> categories; // Kategorie sprzedaży w tym punkcie
    
    public PointOfSale(String id, String nazwa, Integer idUzytkownika, 
                      List<Integer> sellerIds, WorkingHours workingHours, 
                      Set<SalesCategory> categories) {
        this.id = id;
        this.nazwa = nazwa;
        this.idUzytkownika = idUzytkownika;
        this.sellerIds = List.copyOf(sellerIds);
        this.workingHours = workingHours;
        this.categories = Set.copyOf(categories);
    }
    
    public String getId() {
        return id;
    }
    
    public String getNazwa() {
        return nazwa;
    }
    
    public Integer getIdUzytkownika() {
        return idUzytkownika;
    }
    
    public List<Integer> getSellerIds() {
        return sellerIds;
    }
    
    public WorkingHours getWorkingHours() {
        return workingHours;
    }
    
    public Set<SalesCategory> getCategories() {
        return categories;
    }
    
    /**
     * Sprawdza czy punkt jest otwarty w danym dniu tygodnia i godzinie
     */
    public boolean isOpen(DayOfWeek dayOfWeek, LocalTime time) {
        return workingHours.isOpen(dayOfWeek, time);
    }
    
    /**
     * Sprawdza czy dany sprzedawca należy do tego punktu sprzedaży
     */
    public boolean hasSeller(Integer sellerId) {
        return sellerIds.contains(sellerId);
    }
    
    /**
     * Sprawdza czy punkt ma daną kategorię sprzedaży
     */
    public boolean hasCategory(SalesCategory category) {
        return categories.contains(category);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointOfSale that = (PointOfSale) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PointOfSale{" +
                "id='" + id + '\'' +
                ", nazwa='" + nazwa + '\'' +
                ", idUzytkownika=" + idUzytkownika +
                ", sellerIds=" + sellerIds +
                ", workingHours=" + workingHours +
                ", categories=" + categories +
                '}';
    }
}


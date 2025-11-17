package pl.kurs.sogaapplication.service.analysis.concept;

import pl.kurs.sogaapplication.models.business.PointOfSale;

/**
 * Enum reprezentujący koncepty biznesowe (punkty sprzedaży)
 * @deprecated Użyj {@link PointOfSale} zamiast tego enum. 
 *             To enum zostanie usunięte w przyszłości na rzecz nowego modelu biznesowego.
 */
@Deprecated(forRemoval = true)
public enum Koncept {
    KUCHNIA_DOMOWA(11, "Kuchnia Domowa", "KD"),
    RATUSZOWA(null, "Ratuszowa", "RATUSZOWA");

    private final Integer idUzytkownika;
    private final String nazwa;
    private final String pointOfSaleId;

    Koncept(Integer idUzytkownika, String nazwa, String pointOfSaleId) {
        this.idUzytkownika = idUzytkownika;
        this.nazwa = nazwa;
        this.pointOfSaleId = pointOfSaleId;
    }

    public Integer getIdUzytkownika() {
        return idUzytkownika;
    }

    public String getNazwa() {
        return nazwa;
    }
    
    public String getPointOfSaleId() {
        return pointOfSaleId;
    }

    public static Koncept fromIdUzytkownika(Integer idUz) {
        if (idUz != null && idUz == 11) {
            return KUCHNIA_DOMOWA;
        }
        return RATUSZOWA;
    }
    
    public static Koncept fromPointOfSaleId(String pointOfSaleId) {
        if ("KD".equals(pointOfSaleId)) {
            return KUCHNIA_DOMOWA;
        }
        return RATUSZOWA;
    }

    public boolean isKuchniaDomowa() {
        return this == KUCHNIA_DOMOWA;
    }
    
    /**
     * Konwertuje enum do PointOfSale (wymaga wstrzyknięcia PointOfSaleService)
     * Lepsze jest bezpośrednie użycie PointOfSaleService
     */
    @Deprecated
    public String toPointOfSaleId() {
        return pointOfSaleId;
    }
}
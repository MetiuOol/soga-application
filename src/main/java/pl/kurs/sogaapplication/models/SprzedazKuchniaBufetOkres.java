package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;

/**
 * Podsumowanie sprzedaży w podziale na kategorie:
 * - kuchnia
 * - bufet (pozostałe po odjęciu kuchni/opakowań/dowozu)
 * - opakowania
 * - dowóz
 * - suma razem
 */
public record SprzedazKuchniaBufetOkres(
        BigDecimal kuchniaNetto,
        BigDecimal bufetNetto,
        BigDecimal opakowaniaNetto,
        BigDecimal dowozNetto,
        BigDecimal sumaRazem
) {
}


package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Podział sprzedaży dla jednego dnia na kategorie:
 * - kuchnia
 * - bufet
 * - opakowania
 * - dowóz
 * - suma
 */
public record DzienPodzial(
        LocalDate dzien,
        BigDecimal kuchnia,
        BigDecimal bufet,
        BigDecimal opakowania,
        BigDecimal dowoz,
        BigDecimal suma
) {}

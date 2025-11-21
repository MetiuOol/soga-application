package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;

/**
 * DTO dla szczegółowej pozycji sprzedaży.
 * Zawiera informacje o konkretnej pozycji z rachunku.
 */
public record SalesItemDetailDto(
        Long rachunekId,
        String sellerName,
        Integer sellerId,
        Long towarId,
        String towarNazwa,
        Integer towarGrupa,
        BigDecimal ilosc,
        BigDecimal wartoscNetto,
        String category // "kitchen", "buffet", "packaging", "delivery"
) { }



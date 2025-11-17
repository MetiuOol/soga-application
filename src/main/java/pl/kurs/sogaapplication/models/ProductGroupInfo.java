package pl.kurs.sogaapplication.models;

/**
 * DTO do przechowywania informacji o grupie towarów w sprzedaży
 */
public record ProductGroupInfo(
        Integer groupId,
        Long liczbaRachunkow,
        Long liczbaRoznychTowarow,
        java.math.BigDecimal przychodNetto,
        java.math.BigDecimal iloscSprzedana
) {
}





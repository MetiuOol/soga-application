package pl.kurs.sogaapplication.service.analysis.concept;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalizaSprzedazyKD(
        LocalDate dataOd,
        LocalDate dataDo,

        // Sprzedaż na wagę - NA MIEJSCU
        BigDecimal kgNaMiejscu,
        BigDecimal przychodNettoNaMiejscu,
        Integer iloscRachunkowNaMiejscu,

        // Sprzedaż na wagę - NA WYNOS
        BigDecimal kgNaWynos,
        BigDecimal przychodNettoNaWynos,
        Integer iloscRachunkowWynos,

        // Zupy
        Integer iloscZup,
        BigDecimal przychodNettoZupy,

        // Abonamenty
        Integer iloscAbonamentow,
        BigDecimal przychodNettoAbonamenty,

        // Karnety zwykłe
        Integer iloscKarnetowZwyklych,
        BigDecimal przychodNettoKarnetyZwykle,

        // Karnety mięsne
        Integer iloscKarnetowMiesnych,
        BigDecimal przychodNettoKarnetyMiesne,

        // Podsumowanie
        BigDecimal calkowitaPrzychodNetto,
        Integer iloscRachunkowLacznie
) {
    // Metody pomocnicze
    public BigDecimal getSredniaKgNaRachunek() {
        BigDecimal calkowiteKg = kgNaMiejscu.add(kgNaWynos);
        int calkowiteRachunki = iloscRachunkowNaMiejscu + iloscRachunkowWynos;
        if (calkowiteRachunki == 0) return BigDecimal.ZERO;
        return calkowiteKg.divide(BigDecimal.valueOf(calkowiteRachunki), 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getProcentWynos() {
        int calkowiteRachunki = iloscRachunkowNaMiejscu + iloscRachunkowWynos;
        if (calkowiteRachunki == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(iloscRachunkowWynos)
                .divide(BigDecimal.valueOf(calkowiteRachunki), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getCenaNettoZaKg() {
        BigDecimal calkowiteKg = kgNaMiejscu.add(kgNaWynos);
        if (calkowiteKg.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal calkowyPrzychod = przychodNettoNaMiejscu.add(przychodNettoNaWynos);
        return calkowyPrzychod.divide(calkowiteKg, 2, java.math.RoundingMode.HALF_UP);
    }
}
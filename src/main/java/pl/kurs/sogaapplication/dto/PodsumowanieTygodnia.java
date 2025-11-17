package pl.kurs.sogaapplication.dto;

import java.math.BigDecimal;

public class PodsumowanieTygodnia {
    private final int rok;
    private final int numerTygodnia;
    private final BigDecimal sumaNetto;
    private final BigDecimal sumaBrutto;
    private final int liczbaRachunkow;
    private final int lacznaIloscOsob;

    public PodsumowanieTygodnia(int rok, int numerTygodnia,
                                BigDecimal sumaNetto, BigDecimal sumaBrutto,
                                int liczbaRachunkow, int lacznaIloscOsob) {
        this.rok = rok;
        this.numerTygodnia = numerTygodnia;
        this.sumaNetto = sumaNetto;
        this.sumaBrutto = sumaBrutto;
        this.liczbaRachunkow = liczbaRachunkow;
        this.lacznaIloscOsob = lacznaIloscOsob;
    }

    // Getters
    public int getRok() { return rok; }
    public int getNumerTygodnia() { return numerTygodnia; }
    public BigDecimal getSumaNetto() { return sumaNetto; }
    public BigDecimal getSumaBrutto() { return sumaBrutto; }
    public int getLiczbaRachunkow() { return liczbaRachunkow; }
    public int getLacznaIloscOsob() { return lacznaIloscOsob; }

    @Override
    public String toString() {
        return "Tydzień " + numerTygodnia +
                " (" + rok + "): " +
                "sumaNetto=" + sumaNetto +
                ", sumaBrutto=" + sumaBrutto +
                ", liczbaRachunkow=" + liczbaRachunkow +
                ", lacznaIloscOsob=" + lacznaIloscOsob;
    }
}
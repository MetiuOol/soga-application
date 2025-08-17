package pl.kurs.sogaapplication.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "POZRACH")
public class Pozycja {
    @Id
    @Column(name = "ID_POZRACH")
    private Long idPoz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_RACH", referencedColumnName = "ID_RACH")
    private Rachunek rachunek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TW", referencedColumnName = "ID_TW")
    private Towar towar;

    @Column(name = "ILOSC", precision = 12, scale = 3)
    private BigDecimal iloscTowaru;

    public Pozycja() {
    }

    public Pozycja(Rachunek rachunek, Towar towar, BigDecimal iloscTowaru) {
        this.rachunek = rachunek;
        this.towar = towar;
        this.iloscTowaru = iloscTowaru;
    }

    public Long getIdPoz() {
        return idPoz;
    }

    public Rachunek getRachunek() {
        return rachunek;
    }

    public Towar getTowar() {
        return towar;
    }

    public BigDecimal getIloscTowaru() {
        return iloscTowaru;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pozycja pozycja = (Pozycja) o;
        return Objects.equals(idPoz, pozycja.idPoz) && Objects.equals(rachunek, pozycja.rachunek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPoz, rachunek);
    }

//    @Override
//    public String toString() {
//        String nazwa = (towar != null && org.hibernate.Hibernate.isInitialized(towar))
//                ? towar.getNazwaTowaru() : null;
//        return "Pozycja{" +
//                "idPoz=" + idPoz +
//                ", rachunek=" + rachunek +
//                ", towarNazwa=" + nazwa +
//                '}';
//    }
public String toStringShort() {
    // Uwaga: nie odwołujemy się do this.rachunek, żeby nie wywołać rekurencji
    Long twId = null; String twNazwa = null;
    if (towar != null) {
        if (towar instanceof org.hibernate.proxy.HibernateProxy proxy) {
            twId = (Long) proxy.getHibernateLazyInitializer().getIdentifier();
        } else {
            twId = towar.getIdTowaru();
        }
        if (org.hibernate.Hibernate.isInitialized(towar)) {
            twNazwa = towar.getNazwaTowaru();
        }
    }
    return "Poz{" +
            "id=" + idPoz +
            ", towarId=" + twId +
            ", towarNazwa=" + twNazwa +
            ", iloscTowaru=" + iloscTowaru +
            "}";
}

}

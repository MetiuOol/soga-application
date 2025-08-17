package pl.kurs.sogaapplication.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "RACHUNKI")
public class Rachunek implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_RACH")
    private Long id;

    @Column(name = "DATA_ROZ", nullable = false)
    private LocalDateTime dataRozpoczecia;

    @Column(name = "DATA_ZAK", nullable = false)
    private LocalDateTime dataZakonczenia;

    @Column(name = "WART_NU", nullable = false)
    private BigDecimal wartNu;

    @Column(name = "WART_BU", nullable = false)
    private BigDecimal wartBu;

    @Column(name = "IL_OSOB", nullable = false)
    private Integer iloscOsob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_UZ")
    private Uzytkownik uzytkownik;

    @OneToMany(mappedBy = "rachunek", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Pozycja> pozycje;

    public Rachunek() {
    }

    public Rachunek(LocalDateTime dataRozpoczecia, LocalDateTime dataZakonczenia, BigDecimal wartNu, BigDecimal wartBu, Integer iloscOsob, Uzytkownik uzytkownik) {
        this.dataRozpoczecia = dataRozpoczecia;
        this.dataZakonczenia = dataZakonczenia;
        this.wartNu = wartNu;
        this.wartBu = wartBu;
        this.iloscOsob = iloscOsob;
        this.uzytkownik = uzytkownik;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataRozpoczecia() {
        return dataRozpoczecia;
    }

    public LocalDateTime getDataZakonczenia() {
        return dataZakonczenia;
    }

    public BigDecimal getWartNu() {
        return wartNu;
    }

    public BigDecimal getWartBu() {
        return wartBu;
    }

    public Integer getIloscOsob() {
        return iloscOsob;
    }

    public Uzytkownik getUzytkownik() {
        return uzytkownik;
    }

    public List<Pozycja> getPozycje() {
        return pozycje;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rachunek rachunek = (Rachunek) o;
        return Objects.equals(id, rachunek.id) && Objects.equals(dataRozpoczecia, rachunek.dataRozpoczecia) && Objects.equals(dataZakonczenia, rachunek.dataZakonczenia) && Objects.equals(wartNu, rachunek.wartNu) && Objects.equals(wartBu, rachunek.wartBu) && Objects.equals(iloscOsob, rachunek.iloscOsob) && Objects.equals(uzytkownik, rachunek.uzytkownik);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dataRozpoczecia, dataZakonczenia, wartNu, wartBu, iloscOsob, uzytkownik);
    }

    //    @Override
//    public String toString() {
//        Long uzId = null;
//        if (uzytkownik != null) {
//            if (uzytkownik instanceof org.hibernate.proxy.HibernateProxy proxy) {
//                uzId = (Long) proxy.getHibernateLazyInitializer().getIdentifier();
//            } else {
//                uzId = uzytkownik.getId();
//            }
//        }
//
//        String nazwa = (uzytkownik != null && Hibernate.isInitialized(uzytkownik))
//                ? uzytkownik.getNazwaUzytkownika()
//                : null;
//
//        String pozycjeStr = null;
//        if (pozycje != null && Hibernate.isInitialized(pozycje)) {
//            pozycjeStr = pozycje.stream()
//                    .map(Pozycja::toString)
//                    .collect(Collectors.joining(", "));
//        }
//
//        return "Rachunek{" +
//                "id=" + id +
//                ", dataRoz=" + dataRoz +
//                ", dataZam=" + dataZam +
//                ", wartNu=" + wartNu +
//                ", wartBu=" + wartBu +
//                ", iloscOsob=" + iloscOsob +
//                ", uzytkownikId=" + uzId +
//                ", uzytkownikNazwa=" + nazwa +
//                ", pozycje=[" + pozycjeStr + "]" +
//                '}';
    @Override
    public String toString() {
        Integer uzId = null;
        if (uzytkownik != null) {
            if (uzytkownik instanceof org.hibernate.proxy.HibernateProxy proxy) {
                uzId = (Integer) proxy.getHibernateLazyInitializer().getIdentifier();
            } else {
                uzId = uzytkownik.getId();
            }
        }

        String nazwa = (uzytkownik != null && org.hibernate.Hibernate.isInitialized(uzytkownik))
                ? uzytkownik.getNazwaUzytkownika()
                : null;

        String pozycjeStr = null;
        if (pozycje != null && org.hibernate.Hibernate.isInitialized(pozycje)) {
            pozycjeStr = pozycje.stream()
                    .map(Pozycja::toStringShort) // <- ważne: toStringShort, NIE toString!
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        return "Rachunek{" +
                "id=" + id +
                ", dataRozpoczecia=" + dataRozpoczecia +
                ", dataZakonczenia=" + dataZakonczenia +
                ", czasSpedzony=" + (Duration.between(dataRozpoczecia, dataZakonczenia)) +
                ", wartNu=" + wartNu +
                ", wartBu=" + wartBu +
                ", iloscOsob=" + iloscOsob +
                ", uzytkownikId=" + uzId +
                ", uzytkownikNazwa=" + nazwa +
                ", pozycje=[" + pozycjeStr + "]" +
                '}';
    }
}


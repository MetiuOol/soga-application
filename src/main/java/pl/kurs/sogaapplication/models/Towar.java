package pl.kurs.sogaapplication.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TOWARY")
public class Towar {

    @Id
    @Column(name = "ID_TW")
    private Long idTowaru;

    @Column(name = "NAZWA_TW")
    private String nazwaTowaru;

    @Column(name = "ID_GR")
    private Integer idGrupyTowaru;

    public Towar() {
    }

    public Towar(String nazwaTowaru) {
        this.nazwaTowaru = nazwaTowaru;
    }

    public Long getIdTowaru() {
        return idTowaru;
    }

    public String getNazwaTowaru() {
        return nazwaTowaru;
    }

    public Integer getIdGrupyTowaru() {
        return idGrupyTowaru;
    }

    @Override
    public String toString() {
        return "Towar{" +
                "idTowaru=" + idTowaru +
                ", nazwaTowaru='" + nazwaTowaru + '\'' +
                ", idGrupyTowaru='" + idGrupyTowaru + '\'' +
                '}';
    }
}

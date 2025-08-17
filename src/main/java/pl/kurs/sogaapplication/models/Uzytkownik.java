package pl.kurs.sogaapplication.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "UZYTKOWNICY")
public class Uzytkownik implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_UZ")
    private Integer id;


    @Column(name = "NAZWA_UZ")
    private String nazwaUzytkownika;

    public Uzytkownik() {
    }

    public Uzytkownik(String nazwaUzytkownika) {
        this.nazwaUzytkownika = nazwaUzytkownika;
    }

    public Integer getId() {
        return id;
    }

    public String getNazwaUzytkownika() {
        return nazwaUzytkownika;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Uzytkownik that = (Uzytkownik) o;
        return Objects.equals(id, that.id) && Objects.equals(nazwaUzytkownika, that.nazwaUzytkownika);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nazwaUzytkownika);
    }

    @Override
    public String toString() {
        return "Uzytkownik{" +
                "id=" + id +
                ", nazwaUzytkownika='" + nazwaUzytkownika + '\'' +
                '}';
    }
}

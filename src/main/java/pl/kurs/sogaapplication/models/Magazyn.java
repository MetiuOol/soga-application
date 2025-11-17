package pl.kurs.sogaapplication.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Encja reprezentująca magazyny w systemie
 * Zawiera informacje o lokalizacjach i typach magazynów
 */
@Entity
@Table(name = "MAGAZYNY")
public class Magazyn {
    
    @Id
    @Column(name = "ID_MA")
    private Integer id;
    
    @Column(name = "KOD_MA")
    private String kod;
    
    @Column(name = "NAZWA_MA")
    private String nazwa;
    
    @Column(name = "INWENT_STATUS")
    private Integer statusInwentarza;
    
    public Magazyn() {
    }
    
    public Magazyn(Integer id, String kod, String nazwa, Integer statusInwentarza) {
        this.id = id;
        this.kod = kod;
        this.nazwa = nazwa;
        this.statusInwentarza = statusInwentarza;
    }
    
    // Getters
    public Integer getId() {
        return id;
    }
    
    public String getKod() {
        return kod;
    }
    
    public String getNazwa() {
        return nazwa;
    }
    
    public Integer getStatusInwentarza() {
        return statusInwentarza;
    }
    
    // Setters
    public void setId(Integer id) {
        this.id = id;
    }
    
    public void setKod(String kod) {
        this.kod = kod;
    }
    
    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }
    
    public void setStatusInwentarza(Integer statusInwentarza) {
        this.statusInwentarza = statusInwentarza;
    }
    
    /**
     * Sprawdza czy to magazyn kosztów
     */
    public boolean isMagazynKosztow() {
        return id != null && id == 1;
    }
    
    /**
     * Sprawdza czy to magazyn surowców (kuchnia/bufet)
     */
    public boolean isMagazynSurowcow() {
        return id != null && (id == 8 || id == 9 || id == 10);
    }
    
    /**
     * Sprawdza czy to magazyn kuchni
     */
    public boolean isMagazynKuchni() {
        return id != null && id == 8;
    }
    
    /**
     * Sprawdza czy to magazyn bufetu
     */
    public boolean isMagazynBufetu() {
        return id != null && id == 9;
    }
    
    /**
     * Sprawdza czy to magazyn kuchni domowej
     */
    public boolean isMagazynKuchniDomowej() {
        return id != null && id == 10;
    }
    
    @Override
    public String toString() {
        return "Magazyn{" +
                "id=" + id +
                ", kod='" + kod + '\'' +
                ", nazwa='" + nazwa + '\'' +
                ", statusInwentarza=" + statusInwentarza +
                '}';
    }
}







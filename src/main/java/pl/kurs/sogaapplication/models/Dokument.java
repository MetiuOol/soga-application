package pl.kurs.sogaapplication.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Encja reprezentująca dokumenty z systemu magazynowego
 * Zawiera informacje o zakupach, sprzedaży i innych operacjach
 */
@Entity
@Table(name = "DOKUMENTY")
public class Dokument {
    
    @Id
    @Column(name = "ID_DOK")
    private Long id;
    
    @Column(name = "TYP_DOK", nullable = false)
    private String typDokumentu;  // FZ, RR, PZ, KFZ, FS, PA, etc.
    
    @Column(name = "DATA_WST", nullable = false)
    private LocalDate dataWystawienia;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_MA", nullable = false)
    private Magazyn magazyn;
    
    @Column(name = "ID_KATDOK", nullable = false)
    private Integer idKategoriiDokumentu;  // 1=Koszty stałe, 5=paliwo, 2=czynsz, etc.
    
    @Column(name = "WART_NU", nullable = false)
    private BigDecimal wartoscNetto;
    
    @Column(name = "WART_BU", nullable = false)
    private BigDecimal wartoscBrutto;
    
    @Column(name = "WART_VU", nullable = false)
    private BigDecimal wartoscVat;
    
    @Column(name = "STATUS", nullable = false)
    private String status;
    
    @Column(name = "ID_POCHOD")
    private Long idPochodzenia;  // ID dokumentu źródłowego (np. PZ dla FZ)
    
    @Column(name = "NR_ORYGIN")
    private String nrOryginalny;  // Numer oryginalnego dokumentu
    
    @Column(name = "ID_FI")
    private Integer idFirmy;  // ID kontrahenta
    
    @Column(name = "CALY_NR")
    private String calyNumer;  // Pełny numer dokumentu
    
    public Dokument() {
    }
    
    public Dokument(Long id, String typDokumentu, LocalDate dataWystawienia, 
                   Magazyn magazyn, Integer idKategoriiDokumentu,
                   BigDecimal wartoscNetto, BigDecimal wartoscBrutto, BigDecimal wartoscVat,
                   String status) {
        this.id = id;
        this.typDokumentu = typDokumentu;
        this.dataWystawienia = dataWystawienia;
        this.magazyn = magazyn;
        this.idKategoriiDokumentu = idKategoriiDokumentu;
        this.wartoscNetto = wartoscNetto;
        this.wartoscBrutto = wartoscBrutto;
        this.wartoscVat = wartoscVat;
        this.status = status;
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getTypDokumentu() {
        return typDokumentu;
    }
    
    public LocalDate getDataWystawienia() {
        return dataWystawienia;
    }
    
    public Magazyn getMagazyn() {
        return magazyn;
    }
    
    public Integer getIdKategoriiDokumentu() {
        return idKategoriiDokumentu;
    }
    
    public BigDecimal getWartoscNetto() {
        return wartoscNetto;
    }
    
    public BigDecimal getWartoscBrutto() {
        return wartoscBrutto;
    }
    
    public BigDecimal getWartoscVat() {
        return wartoscVat;
    }
    
    public String getStatus() {
        return status;
    }
    
    public Long getIdPochodzenia() {
        return idPochodzenia;
    }
    
    public String getNrOryginalny() {
        return nrOryginalny;
    }
    
    public Integer getIdFirmy() {
        return idFirmy;
    }
    
    public String getCalyNumer() {
        return calyNumer;
    }
    
    // Setters
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setTypDokumentu(String typDokumentu) {
        this.typDokumentu = typDokumentu;
    }
    
    public void setDataWystawienia(LocalDate dataWystawienia) {
        this.dataWystawienia = dataWystawienia;
    }
    
    public void setMagazyn(Magazyn magazyn) {
        this.magazyn = magazyn;
    }
    
    public void setIdKategoriiDokumentu(Integer idKategoriiDokumentu) {
        this.idKategoriiDokumentu = idKategoriiDokumentu;
    }
    
    public void setWartoscNetto(BigDecimal wartoscNetto) {
        this.wartoscNetto = wartoscNetto;
    }
    
    public void setWartoscBrutto(BigDecimal wartoscBrutto) {
        this.wartoscBrutto = wartoscBrutto;
    }
    
    public void setWartoscVat(BigDecimal wartoscVat) {
        this.wartoscVat = wartoscVat;
    }
    
    public void setIdPochodzenia(Long idPochodzenia) {
        this.idPochodzenia = idPochodzenia;
    }
    
    public void setNrOryginalny(String nrOryginalny) {
        this.nrOryginalny = nrOryginalny;
    }
    
    public void setIdFirmy(Integer idFirmy) {
        this.idFirmy = idFirmy;
    }
    
    public void setCalyNumer(String calyNumer) {
        this.calyNumer = calyNumer;
    }
    
    /**
     * Sprawdza czy dokument jest zakupem
     */
    public boolean isZakup() {
        return "FZ".equals(typDokumentu) || "RR".equals(typDokumentu) || 
               "PZ".equals(typDokumentu) || "KFZ".equals(typDokumentu);
    }
    
    /**
     * Sprawdza czy dokument jest sprzedażą
     */
    public boolean isSprzedaz() {
        return "FS".equals(typDokumentu) || "PA".equals(typDokumentu) || 
               "KFS".equals(typDokumentu);
    }
    
    /**
     * Sprawdza czy dokument dotyczy surowców (kuchnia/bufet)
     */
    public boolean isSurowce() {
        return magazyn != null && magazyn.isMagazynSurowcow();
    }
    
    /**
     * Sprawdza czy dokument dotyczy kosztów operacyjnych
     */
    public boolean isKosztyOperacyjne() {
        return magazyn != null && magazyn.isMagazynKosztow();
    }
    
    @Override
    public String toString() {
        return "Dokument{" +
                "id=" + id +
                ", typDokumentu='" + typDokumentu + '\'' +
                ", dataWystawienia=" + dataWystawienia +
                ", magazyn=" + (magazyn != null ? magazyn.getNazwa() : "null") +
                ", idKategoriiDokumentu=" + idKategoriiDokumentu +
                ", wartoscNetto=" + wartoscNetto +
                ", wartoscBrutto=" + wartoscBrutto +
                ", wartoscVat=" + wartoscVat +
                '}';
    }
}

package pl.kurs.sogaapplication.service.analysis.concept;

public enum TypSprzedazyKD {
    NA_WAGE(4469, 4593, "Sprzedaż na wagę"),
    ZUPA(4468, "Zupy"),
    ABONAMENT(4794, "Abonament miesięczny z dowozem"),
    KARNET_ZWYKLY(4472, "Karnet 16 obiadów"),
    KARNET_MIESNY(5157, "Karnet 16 obiadów - mięsne"),
    OPAKOWANIE(951, 6438, "Opakowanie (na wynos)");

    private final Integer[] idTowarow;
    private final String opis;

    TypSprzedazyKD(Integer idTowaru, String opis) {
        this.idTowarow = new Integer[]{idTowaru};
        this.opis = opis;
    }

    TypSprzedazyKD(Integer idTowaru1, Integer idTowaru2, String opis) {
        this.idTowarow = new Integer[]{idTowaru1, idTowaru2};
        this.opis = opis;
    }

    public Integer[] getIdTowarow() {
        return idTowarow;
    }

    public String getOpis() {
        return opis;
    }

    public boolean contains(Integer idTowaru) {
        if (idTowaru == null) return false;
        for (Integer id : idTowarow) {
            if (id.equals(idTowaru)) return true;
        }
        return false;
    }
}

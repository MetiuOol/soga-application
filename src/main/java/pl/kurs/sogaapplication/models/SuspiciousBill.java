package pl.kurs.sogaapplication.models;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Model reprezentujący podejrzany rachunek
 */
public record SuspiciousBill(
        Long billId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Duration duration,
        BigDecimal amount,
        String sellerName,
        Integer sellerId,
        String reason,
        String severity
) {
    
    /**
     * Tworzy SuspiciousBill z Rachunek i powodem
     */
    public static SuspiciousBill from(Rachunek rachunek, String reason, String severity) {
        Duration duration = Duration.between(rachunek.getDataRozpoczecia(), rachunek.getDataZakonczenia());
        
        String sellerName = "Nieznany";
        Integer sellerId = null;
        if (rachunek.getUzytkownik() != null) {
            sellerId = rachunek.getUzytkownik().getId();
            if (org.hibernate.Hibernate.isInitialized(rachunek.getUzytkownik())) {
                sellerName = rachunek.getUzytkownik().getNazwaUzytkownika();
            }
        }
        
        return new SuspiciousBill(
                rachunek.getId(),
                rachunek.getDataRozpoczecia(),
                rachunek.getDataZakonczenia(),
                duration,
                rachunek.getWartNu(),
                sellerName,
                sellerId,
                reason,
                severity
        );
    }
    
    /**
     * Zwraca czytelny opis rachunku
     */
    public String getDescription() {
        return String.format("Rachunek #%d - %s zł, %s, %s", 
                billId, amount, sellerName, getDurationFormatted());
    }
    
    /**
     * Zwraca sformatowany czas trwania
     */
    public String getDurationFormatted() {
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "min";
        }
    }
    
    /**
     * Sprawdza czy rachunek jest bardzo podejrzany (bardzo wysoka kwota + bardzo krótki czas)
     */
    public boolean isVerySuspicious(BigDecimal highAmount, int shortDurationMinutes) {
        return amount.compareTo(highAmount) > 0 && duration.toMinutes() < shortDurationMinutes;
    }
}







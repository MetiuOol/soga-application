package pl.kurs.sogaapplication.models.business;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

/**
 * Model reprezentujący godziny pracy punktu sprzedaży
 * Określa kiedy punkt jest otwarty w poszczególnych dniach tygodnia
 */
public class WorkingHours {
    
    private final Map<DayOfWeek, TimeRange> hoursByDay;
    
    public WorkingHours(Map<DayOfWeek, TimeRange> hoursByDay) {
        this.hoursByDay = Map.copyOf(hoursByDay);
    }
    
    /**
     * Sprawdza czy punkt jest otwarty w danym dniu tygodnia i godzinie
     */
    public boolean isOpen(DayOfWeek dayOfWeek, LocalTime time) {
        TimeRange range = hoursByDay.get(dayOfWeek);
        if (range == null) {
            return false; // Zamknięte jeśli nie ma zdefiniowanych godzin
        }
        return range.contains(time);
    }
    
    /**
     * Zwraca zakres godzin dla danego dnia tygodnia
     */
    public TimeRange getHoursForDay(DayOfWeek dayOfWeek) {
        return hoursByDay.get(dayOfWeek);
    }
    
    /**
     * Sprawdza czy punkt jest otwarty w danym dniu tygodnia
     */
    public boolean isOpenOnDay(DayOfWeek dayOfWeek) {
        return hoursByDay.containsKey(dayOfWeek);
    }
    
    public Map<DayOfWeek, TimeRange> getHoursByDay() {
        return hoursByDay;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingHours that = (WorkingHours) o;
        return Objects.equals(hoursByDay, that.hoursByDay);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(hoursByDay);
    }
    
    @Override
    public String toString() {
        return "WorkingHours{" +
                "hoursByDay=" + hoursByDay +
                '}';
    }
    
    /**
     * Zakres czasowy (od-do)
     */
    public record TimeRange(LocalTime openTime, LocalTime closeTime) {
        public TimeRange {
            if (openTime == null || closeTime == null) {
                throw new IllegalArgumentException("Open and close times cannot be null");
            }
            if (closeTime.isBefore(openTime)) {
                throw new IllegalArgumentException("Close time cannot be before open time");
            }
        }
        
        /**
         * Sprawdza czy czas jest w zakresie
         */
        public boolean contains(LocalTime time) {
            return !time.isBefore(openTime) && !time.isAfter(closeTime);
        }
        
        /**
         * Liczy liczbę godzin pracy
         */
        public long getHours() {
            return java.time.Duration.between(openTime, closeTime).toHours();
        }
    }
}






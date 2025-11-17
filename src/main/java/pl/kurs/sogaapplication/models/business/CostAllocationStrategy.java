package pl.kurs.sogaapplication.models.business;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Strategia alokacji kosztów między punktami sprzedaży
 * Określa jak dzielić wspólne koszty między różne punkty
 */
public interface CostAllocationStrategy {
    
    /**
     * Alokuje koszty między punktami sprzedaży na podstawie danych
     * 
     * @param totalCost Całkowity koszt do podziału
     * @param salesByPoint Przychody dla każdego punktu sprzedaży
     * @param workingHoursByPoint Godziny pracy dla każdego punktu
     * @return Mapę punkt sprzedaży -> przydzielony koszt
     */
    Map<String, BigDecimal> allocateCost(BigDecimal totalCost,
                                        Map<String, BigDecimal> salesByPoint,
                                        Map<String, Long> workingHoursByPoint);
    
    /**
     * Strategia proporcjonalna do sprzedaży
     * Koszty dzielone w proporcji do przychodu każdego punktu
     */
    class ProportionalToSalesStrategy implements CostAllocationStrategy {
        
        @Override
        public Map<String, BigDecimal> allocateCost(BigDecimal totalCost,
                                                   Map<String, BigDecimal> salesByPoint,
                                                   Map<String, Long> workingHoursByPoint) {
            BigDecimal totalSales = salesByPoint.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalSales.compareTo(BigDecimal.ZERO) == 0) {
                // Jeśli brak sprzedaży, dziel równo
                BigDecimal equalShare = totalCost.divide(
                        BigDecimal.valueOf(salesByPoint.size()), 
                        2, 
                        java.math.RoundingMode.HALF_UP);
                return salesByPoint.keySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                point -> point,
                                point -> equalShare));
            }
            
            return salesByPoint.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                BigDecimal sales = entry.getValue();
                                BigDecimal proportion = sales.divide(totalSales, 4, java.math.RoundingMode.HALF_UP);
                                return totalCost.multiply(proportion).setScale(2, java.math.RoundingMode.HALF_UP);
                            }));
        }
    }
    
    /**
     * Strategia proporcjonalna do godzin pracy
     * Koszty dzielone w proporcji do liczby godzin pracy każdego punktu
     */
    class ProportionalToHoursStrategy implements CostAllocationStrategy {
        
        @Override
        public Map<String, BigDecimal> allocateCost(BigDecimal totalCost,
                                                   Map<String, BigDecimal> salesByPoint,
                                                   Map<String, Long> workingHoursByPoint) {
            long totalHours = workingHoursByPoint.values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
            
            if (totalHours == 0) {
                // Jeśli brak godzin, dziel równo
                BigDecimal equalShare = totalCost.divide(
                        BigDecimal.valueOf(workingHoursByPoint.size()), 
                        2, 
                        java.math.RoundingMode.HALF_UP);
                return workingHoursByPoint.keySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                point -> point,
                                point -> equalShare));
            }
            
            return workingHoursByPoint.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                long hours = entry.getValue();
                                BigDecimal proportion = BigDecimal.valueOf(hours)
                                        .divide(BigDecimal.valueOf(totalHours), 4, java.math.RoundingMode.HALF_UP);
                                return totalCost.multiply(proportion).setScale(2, java.math.RoundingMode.HALF_UP);
                            }));
        }
    }
    
    /**
     * Strategia hybrydowa
     * Część kosztów proporcjonalnie do sprzedaży, część do godzin pracy
     */
    class HybridStrategy implements CostAllocationStrategy {
        
        private final BigDecimal salesWeight; // Waga dla proporcji sprzedaży (0.0 - 1.0)
        private final BigDecimal hoursWeight; // Waga dla proporcji godzin (0.0 - 1.0)
        
        public HybridStrategy(BigDecimal salesWeight, BigDecimal hoursWeight) {
            if (salesWeight.add(hoursWeight).compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Sum of weights must equal 1.0");
            }
            this.salesWeight = salesWeight;
            this.hoursWeight = hoursWeight;
        }
        
        @Override
        public Map<String, BigDecimal> allocateCost(BigDecimal totalCost,
                                                   Map<String, BigDecimal> salesByPoint,
                                                   Map<String, Long> workingHoursByPoint) {
            CostAllocationStrategy salesStrategy = new ProportionalToSalesStrategy();
            CostAllocationStrategy hoursStrategy = new ProportionalToHoursStrategy();
            
            BigDecimal salesAllocation = totalCost.multiply(salesWeight);
            BigDecimal hoursAllocation = totalCost.multiply(hoursWeight);
            
            Map<String, BigDecimal> salesBased = salesStrategy.allocateCost(
                    salesAllocation, salesByPoint, workingHoursByPoint);
            Map<String, BigDecimal> hoursBased = hoursStrategy.allocateCost(
                    hoursAllocation, salesByPoint, workingHoursByPoint);
            
            return salesByPoint.keySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            point -> point,
                            point -> salesBased.get(point).add(hoursBased.get(point))));
        }
    }
}






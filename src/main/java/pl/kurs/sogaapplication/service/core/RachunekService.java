package pl.kurs.sogaapplication.service.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SellerKey;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Podstawowy serwis do operacji na rachunkach (CRUD)
 * Odpowiada tylko za podstawowe operacje na danych
 */
@Service
public class RachunekService {

    private final RachunekJpaRepository repo;

    public RachunekService(RachunekJpaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Rachunek findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rachunku id=" + id));
    }

    @Transactional(readOnly = true)
    public Rachunek findByIdWithUser(Long id) {
        return repo.findByIdWithUzytkownik(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rachunku id=" + id));
    }

    @Transactional(readOnly = true)
    public Rachunek findByIdWithUserAndPositions(Long id) {
        return repo.findByIdWithUserPozycjeAndTowar(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rachunku id=" + id));
    }

    @Transactional(readOnly = true)
    public List<Rachunek> findByDateRange(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay(); // półotwarty przedział
        return repo.findAllByDataZamBetweenWithUserAndPozycje(fromDateTime, toDateTime);
    }

    @Transactional(readOnly = true)
    public List<Rachunek> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return repo.findAllByDataZamBetweenWithUserAndPozycje(from, to);
    }

    /**
     * Zwraca liczbę rachunków w danym dniu dla wskazanych sprzedawców (po DATA_ROZ).
     */
    @Transactional(readOnly = true)
    public long countByDateAndSellers(LocalDate date, List<Integer> sellerIds) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return repo.liczbaRachunkowBySellers(from, to, sellerIds);
    }

    /**
     * Zwraca liczbę rachunków w zadanym zakresie dat (włącznie) dla wskazanych sprzedawców (po DATA_ROZ).
     */
    @Transactional(readOnly = true)
    public long countByRangeAndSellers(LocalDate fromDate, LocalDate toDate, List<Integer> sellerIds) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay();
        return repo.liczbaRachunkowBySellers(from, to, sellerIds);
    }

    @Transactional(readOnly = true)
    public Map<SellerKey, List<Rachunek>> groupBySeller(LocalDate from, LocalDate to) {
        List<Rachunek> rachunki = findByDateRange(from, to);
        return rachunki.stream()
                .collect(Collectors.groupingBy(rachunek -> {
                    var uzytkownik = rachunek.getUzytkownik();
                    Integer id = (uzytkownik instanceof org.hibernate.proxy.HibernateProxy proxy)
                            ? (Integer) proxy.getHibernateLazyInitializer().getIdentifier()
                            : uzytkownik.getId();
                    String nazwa = org.hibernate.Hibernate.isInitialized(uzytkownik) 
                            ? uzytkownik.getNazwaUzytkownika() : null;
                    return new SellerKey(id, nazwa);
                }));
    }

    @Transactional
    public Rachunek save(Rachunek rachunek) {
        return repo.save(rachunek);
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return repo.count();
    }
}






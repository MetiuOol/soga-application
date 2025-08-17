package pl.kurs.sogaapplication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SellerKey;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RachunekService {

    private final RachunekJpaRepository repo;

    public RachunekService(RachunekJpaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Rachunek getWithUser(Long id) {
        return repo.findByIdWithUzytkownik(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rachunku id=" + id));
    }

//    @Transactional(readOnly = true)
//    public Rachunek getFull(Long id) {
//        return repo.findByIdWithUserAndPozycje(id).orElseThrow();
//    }

    @Transactional(readOnly = true)
    public Rachunek getFull(Long id) {
        return repo.findByIdWithUserPozycjeAndTowar(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<Rachunek> getRachunki(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay(); // półotwarty przedział
        return repo.findAllByDataZamBetweenWithUserAndPozycje(from, to);
    }

    @Transactional(readOnly = true)
    public Map<SellerKey, List<Rachunek>> rachunkiPogrupowanePoSprzedawcy(LocalDate start, LocalDate end) {
        List<Rachunek> lista = getRachunki(start, end);
        return lista.stream()
                .collect(Collectors.groupingBy(x -> {
                    var u = x.getUzytkownik();
                    Integer id = (u instanceof org.hibernate.proxy.HibernateProxy p)
                            ? (Integer) p.getHibernateLazyInitializer().getIdentifier()
                            : u.getId();
                    String nazwa = org.hibernate.Hibernate.isInitialized(u) ? u.getNazwaUzytkownika() : null;
                    return new SellerKey(id, nazwa);

                }));
    }
    @Transactional(readOnly = true)
    public List<ObrotSprzedawcyGodzina> obrotyPoSprzedawcyIGodzinie(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        List<ObrotSprzedawcyGodzina> obrotSprzedawcyGodzinas = repo.sumyGodzinowe(from, to);


        return obrotSprzedawcyGodzinas.stream()
                .map(r -> new ObrotSprzedawcyGodzina(
                        r.sellerId(),
                        r.sellerName(),
                        r.godzina(),      // SMALLINT -> Integer przeleci
                        r.suma()))
                .toList();


    }
    @Transactional(readOnly = true)
    public void wypisz(List<ObrotSprzedawcyGodzina> dane) {
        NumberFormat kwota = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        kwota.setMinimumFractionDigits(2);
        kwota.setMaximumFractionDigits(2);

        Map<SellerKey, List<ObrotSprzedawcyGodzina>> bySeller =
                dane.stream()
                        .collect(Collectors.groupingBy(
                                d -> new SellerKey(d.sellerId(), d.sellerName()),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        bySeller.forEach((seller, list) -> {
            System.out.printf("%nSprzedawca: %s (%s)%n", seller.nazwa(), seller.id());

            list.stream()
                    .sorted(Comparator.comparingInt(d -> d.godzina().intValue()))
                    .forEach(d -> System.out.printf("  %02d:00  —  %10s%n",
                            d.godzina(), kwota.format(d.suma())));
            var sumaDlaSprzedawcy = list.stream()
                    .map(ObrotSprzedawcyGodzina::suma)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            System.out.printf("  Razem: %s%n", kwota.format(sumaDlaSprzedawcy));
        });



    }
}




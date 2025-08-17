package pl.kurs.sogaapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pl.kurs.sogaapplication.models.ObrotNaGodzine;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SellerKey;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.RachunekService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class SogaApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SogaApplication.class, args);
        RachunekJpaRepository rachunekJpaRepository = ctx.getBean(RachunekJpaRepository.class);
        RachunekService rachunekService = ctx.getBean(RachunekService.class);
//        List<Rachunek> allByDataRozBefore = rachunekJpaRepository.findAllByDataZamBetween(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3));
//        System.out.println("allByDataRozBefore.size() = " + allByDataRozBefore.size());
//        allByDataRozBefore.forEach(System.out::println);
        Rachunek rachunekById = rachunekJpaRepository.findRachunekById(259445L);
        System.out.println("rachunekById = " + rachunekById);

        var rachunek = rachunekService.getFull(259446L);
        System.out.println(rachunek); // teraz toString() pokaże nazwę
//        List<Rachunek> styczenRachunki = rachunekService.getRachunki(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
//        styczenRachunki.forEach(System.out::println);
        Map<SellerKey, List<Rachunek>> mapaPogrupowanaPoSprzedawcyStyczen =
                rachunekService.rachunkiPogrupowanePoSprzedawcy(
                        LocalDate.of(2016, 1, 1),
                        LocalDate.of(2016, 1, 31));
        mapaPogrupowanaPoSprzedawcyStyczen.forEach((seller, rachunki) -> {
            System.out.println(seller.nazwa() + " -> " + rachunki.size());
        });
        List<ObrotSprzedawcyGodzina> obrotSprzedawcyGodzinas = rachunekService.obrotyPoSprzedawcyIGodzinie(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        rachunekService.wypisz(obrotSprzedawcyGodzinas);
    }
}



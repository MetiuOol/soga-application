package pl.kurs.sogaapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import pl.kurs.sogaapplication.dto.PodsumowanieTygodnia;
import pl.kurs.sogaapplication.models.DzienPodzial;
import pl.kurs.sogaapplication.models.ObrotDzien;
import pl.kurs.sogaapplication.models.ObrotDzienGodzinaView;
import pl.kurs.sogaapplication.models.ObrotNaGodzine;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SellerKey;
import pl.kurs.sogaapplication.models.SprzedazKuchniaBufetOkres;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;
import pl.kurs.sogaapplication.service.RachunekService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
//        Rachunek rachunekById = rachunekJpaRepository.findRachunekById(259445L);
//        System.out.println("rachunekById = " + rachunekById);
//        var rachunek = rachunekService.getFull(259446L);
//        System.out.println(rachunek); // teraz toString() pokaże nazwę
//        List<Rachunek> styczenRachunki = rachunekService.getRachunki(LocalDate.of(2025, 3, 14), LocalDate.of(2025, 3, 14));
//        styczenRachunki.forEach(System.out::println);
//        Map<SellerKey, List<Rachunek>> mapaPogrupowanaPoSprzedawcyStyczen =
//                rachunekService.rachunkiPogrupowanePoSprzedawcy(
//                        LocalDate.of(2016, 1, 1),
//                        LocalDate.of(2016, 1, 31));
//        mapaPogrupowanaPoSprzedawcyStyczen.forEach((seller, rachunki) -> {
//            System.out.println(seller.nazwa() + " -> " + rachunki.size());
//        });
        List<ObrotSprzedawcyGodzina> obrotSprzedawcyGodzinas = rachunekService.obrotyPoSprzedawcyIGodzinie(
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 1)
        );
//

//        List<ObrotDzienGodzinaView> wyniki = rachunekService.obrotyDzienGodzina(
//                LocalDate.of(2025, 7, 1),
//                LocalDate.of(2025, 7, 1),
//                List.of(2, 3, 4, 6, 8, 9, 12, 13, 14, 15, 16));
//        rachunekService.soutDlaObrotDzienGodzina(wyniki);
//
//        List<ObrotDzien> obrotDziens = rachunekService.obrotyDzien(
//                LocalDate.of(2025, 7, 1),
//                LocalDate.of(2025, 7, 31),
//                List.of(2, 3, 4, 6, 8, 9, 12, 13, 14, 15, 16));
//                List.of(11, 10));
//
//        rachunekService.soutDlaObrotDzien(obrotDziens);

//        List<PodsumowanieTygodnia> raport = rachunekService.podsumujCalyRok(2024);
//        raport.forEach(System.out::println);


        /**
         * SPRZEDAZ KUCHNIA BUFET START
        WYPISUJE SUME NETTO SPRZEDAZY W DANYM OKRESIE Z PODZIALEM NA KUCHNIE I BUFET
         */
        List<Integer> GRUPY_KUCHNIA = List.of(39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 56, 79, 80, 82, 84, 85, 86, 87, 88, 97, 105, 106, 111, 112, 113, 119, 120, 126, 127, 128, 129);

        SprzedazKuchniaBufetOkres sumy =
                rachunekService.policz(LocalDateTime .of(2025, 1, 1, 0, 0 ,1), LocalDateTime.of(2025, 1, 31, 23, 59, 59), GRUPY_KUCHNIA);

        BigDecimal kuchnia = sumy.kuchniaNetto();
        BigDecimal bufet = sumy.bufetNetto();

        System.out.println("sprzedaz kuchnia " + kuchnia + ", sprzedaz bufet " + bufet + ", suma = " + kuchnia.add(bufet));
        /**
         * SPRZEDAZ KUCHNIA BUFET KONIEC
         */

//        var sellerIds = java.util.List.of(1, 2, 3, 4, 5, 6, 8, 9, 12, 13, 14, 15, 16, 17);           // <- Twoja lista użytkowników
        var sellerIds = java.util.List.of(11);           // <- Twoja lista użytkowników
        var start      = java.time.LocalDate.of(2025, 7, 1); // podajesz tylko 1 lipca
        var wyniki     = rachunekService.policzMiesiacDziennie(start, sellerIds, GRUPY_KUCHNIA);

        wyniki.forEach(w ->
                System.out.println(w.dzien() + " | kuchnia=" + w.kuchnia() +
                        " | bufet=" + w.bufet() + " | razem= " + w.suma())
        );

// jeśli chcesz jeszcze sumę miesiąca:
        var kuchniaSum = wyniki.stream().map(DzienPodzial::kuchnia).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var bufetSum   = wyniki.stream().map(DzienPodzial::bufet).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        System.out.println("MIESIĄC: kuchnia=" + kuchniaSum + " | bufet=" + bufetSum + " | razem= " + kuchniaSum.add(bufetSum));


    }

}





package pl.kurs.sogaapplication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kurs.sogaapplication.dto.PodsumowanieTygodnia;
import pl.kurs.sogaapplication.models.DzienPodzial;
import pl.kurs.sogaapplication.models.ObrotDzien;
import pl.kurs.sogaapplication.models.ObrotDzienGodzinaView;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Rachunek;
import pl.kurs.sogaapplication.models.SellerKey;
import pl.kurs.sogaapplication.models.SprzedazKuchniaBufetOkres;
import pl.kurs.sogaapplication.repositories.RachunekJpaRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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

    @Transactional(readOnly = true)
    public List<ObrotDzienGodzinaView> obrotyDzienGodzina(
            LocalDate start, LocalDate endInclusive, Collection<Integer> sellerIds) {

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toEx = endInclusive.plusDays(1).atStartOfDay();
        return repo.sumyDzienGodzina(from, toEx, sellerIds);
    }

    @Transactional(readOnly = true)
    public List<ObrotDzien> obrotyDzien(
            LocalDate start, LocalDate endInclusive, Collection<Integer> sellerIds) {

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toEx = endInclusive.plusDays(1).atStartOfDay();
        return repo.sumyDzien(from, toEx, sellerIds);
    }

    @Transactional(readOnly = true)
    public void soutDlaObrotDzien(List<ObrotDzien> wyniki) {
//        Map<String, Map<Integer, List<ObrotDzien>>> bySellerThenDay =
//                wyniki.stream().collect(Collectors.groupingBy(
//                        ObrotDzien::getSellerName,
//                        LinkedHashMap::new,
//                        Collectors.groupingBy(ObrotDzien::getDzien, TreeMap::new, Collectors.toList())
//                ));
//
//        String[] dni = {"Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sb"};
//        NumberFormat kwota = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
//        kwota.setMinimumFractionDigits(2);
//        kwota.setMaximumFractionDigits(2);
//
//        bySellerThenDay.forEach((seller, mapDay) -> {
//            System.out.printf("%nSprzedawca: %s%n", seller);
//
//            mapDay.forEach((dzien, lista) -> {
//                // suma z całego dnia
//                BigDecimal sumaDnia = lista.stream()
//                        .map(ObrotDzien::getSuma)
//                        .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                System.out.printf("  %s — %s%n", dni[dzien], kwota.format(sumaDnia));
//            });
//
//            // suma łączna dla całego sprzedawcy
//            BigDecimal sumaSprzedawcy = mapDay.values().stream()
//                    .flatMap(List::stream)
//                    .map(ObrotDzien::getSuma)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            System.out.printf("  Razem: %s%n", kwota.format(sumaSprzedawcy));
//        });
        String[] dni = {"Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sb"};
        NumberFormat kwota = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        kwota.setMinimumFractionDigits(2);
        kwota.setMaximumFractionDigits(2);

        // grupowanie: dzień -> godzina -> suma
        Map<Integer, Map<Integer, BigDecimal>> sumaPerDzienGodzina = wyniki.stream()
                .collect(Collectors.groupingBy(
                        ObrotDzien::getDzien, TreeMap::new,
                        Collectors.groupingBy(
                                ObrotDzien::getGodzina, TreeMap::new,
                                Collectors.reducing(BigDecimal.ZERO, ObrotDzien::getSuma, BigDecimal::add)
                        )
                ));

        // drukowanie
        sumaPerDzienGodzina.forEach((dzien, mapaGodzin) -> {
            System.out.printf("%n%s:%n", dni[dzien]);

            mapaGodzin.forEach((godzina, suma) ->
                    System.out.printf("  %02d:00 — %10s%n", godzina, kwota.format(suma)));

            // suma dnia
            BigDecimal sumaDnia = mapaGodzin.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.printf("  Razem: %s%n", kwota.format(sumaDnia));
        });

        // suma całkowita
        BigDecimal sumaRazem = sumaPerDzienGodzina.values().stream()
                .flatMap(m -> m.values().stream())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.printf("%nRazem wszystkie dni: %s%n", kwota.format(sumaRazem));
        System.out.println("          ");
    }

    @Transactional(readOnly = true)
    public void soutDlaObrotDzienGodzina(List<ObrotDzienGodzinaView> wyniki) {
        Map<String, Map<Integer, List<ObrotDzienGodzinaView>>> bySellerThenDay =
                wyniki.stream().collect(Collectors.groupingBy(
                        ObrotDzienGodzinaView::getSellerName,
                        LinkedHashMap::new,
                        Collectors.groupingBy(ObrotDzienGodzinaView::getDzien, TreeMap::new, Collectors.toList())
                ));

        String[] dni = {"Nd", "Pn", "Wt", "Śr", "Cz", "Pt", "Sb"};
        NumberFormat kwota = NumberFormat.getNumberInstance(new Locale("pl", "PL"));
        kwota.setMinimumFractionDigits(2);
        kwota.setMaximumFractionDigits(2);

        bySellerThenDay.forEach((seller, mapDay) -> {
            System.out.printf("%nSprzedawca: %s%n", seller);
            mapDay.forEach((dzien, lista) -> {
                System.out.printf("  %s:%n", dni[dzien]);
                lista.stream()
                        .sorted(Comparator.comparing(ObrotDzienGodzinaView::getGodzina))
                        .forEach(r -> System.out.printf("    %02d:00  —  %10s%n",
                                r.getGodzina(), kwota.format(r.getSuma())));
            });
        });
    }
    @Transactional(readOnly = true)
    public List<PodsumowanieTygodnia> podsumujCalyRok(int rok) {
        List<PodsumowanieTygodnia> podsumowania = new ArrayList<>();

        LocalDate start = LocalDate.of(rok, 1, 1);
        int numerTygodnia = 1;

        while (start.getYear() == rok) {
            LocalDate endDate = start.plusDays(6); // 7 dniowy zakres
            if (endDate.getYear() > rok) {
                endDate = LocalDate.of(rok, 12, 31); // ostatni dzień roku
            }

            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Rachunek> rachunki = repo.findByDataRozpoczeciaBetween(startDateTime, endDateTime);

            BigDecimal sumaNetto = rachunki.stream()
                    .map(Rachunek::getWartNu)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sumaBrutto = rachunki.stream()
                    .map(Rachunek::getWartBu)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int liczbaOsob = rachunki.stream()
                    .mapToInt(Rachunek::getIloscOsob)
                    .sum();

            podsumowania.add(new PodsumowanieTygodnia(
                    rok,
                    numerTygodnia,
                    sumaNetto,
                    sumaBrutto,
                    rachunki.size(),
                    liczbaOsob
            ));


            // przejście do kolejnego tygodnia
            start = start.plusDays(7);
            numerTygodnia++;
        }

        return podsumowania;
    }

    @Transactional(readOnly = true)
    public SprzedazKuchniaBufetOkres policz(LocalDateTime from, LocalDateTime to, Collection<Integer> kitchenGroupIds) {
        BigDecimal razem   = repo.sumaRazem(from, to);
        BigDecimal kuchnia = repo.sumaKuchnia(from, to, kitchenGroupIds);
        BigDecimal bufet   = razem.subtract(kuchnia); // wszystko poza kuchnią

        return new SprzedazKuchniaBufetOkres(kuchnia, bufet, razem);
    }

    public List<DzienPodzial> policzMiesiacDziennie(LocalDate firstDayOfMonth,
                                                    Collection<Integer> sellerIds,
                                                    Collection<Integer> kitchenGroupIds) {
        var last = firstDayOfMonth.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        var out = new java.util.ArrayList<DzienPodzial>();

        for (var d = firstDayOfMonth; !d.isAfter(last); d = d.plusDays(1)) {
            var from = d.atStartOfDay();
            var to   = d.plusDays(1).atStartOfDay();

            var suma    = repo.sumaRazemBySellers(from, to, sellerIds);
            var kuchnia = repo.sumaKuchniaBySellers(from, to, kitchenGroupIds, sellerIds);
            var bufet   = suma.subtract(kuchnia);

            out.add(new DzienPodzial(d, kuchnia, bufet, suma));

        }
        try {
            String xml = buildXml(out, firstDayOfMonth, sellerIds);
            String fname = String.format("sprzedaz-%d-%02d-%s.xml",
                    firstDayOfMonth.getYear(),
                    firstDayOfMonth.getMonthValue(),
                    joinIds(sellerIds));
            Files.writeString(Path.of(fname), xml, StandardCharsets.UTF_8);
            System.out.println("Zapisano XML: " + Path.of(fname).toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Błąd zapisu XML: " + e.getMessage());
        }

        return out;


    }
    private static String joinIds(Collection<Integer> ids) {
        return ids == null || ids.isEmpty()
                ? ""
                : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String buildXml(List<DzienPodzial> rows, LocalDate from, Collection<Integer> sellerIds) {
        var last = from.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        String ids = sellerIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<sprzedazMiesiac from=\"").append(from).append("\" to=\"").append(last)
                .append("\" sellerIds=\"").append(ids).append("\">\n");
        for (var r : rows) {
            sb.append("  <dzien data=\"").append(r.dzien()).append("\">\n");
            sb.append("    <kuchnia>").append(r.kuchnia().toPlainString()).append("</kuchnia>\n");
            sb.append("    <bufet>").append(r.bufet().toPlainString()).append("</bufet>\n");
            sb.append("    <suma>").append(r.suma().toPlainString()).append("</suma>\n");
            sb.append("  </dzien>\n");
        }
        sb.append("</sprzedazMiesiac>\n");
        return sb.toString();


    }

}




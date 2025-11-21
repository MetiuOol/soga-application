package pl.kurs.sogaapplication.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.kurs.sogaapplication.models.Rachunek;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface RachunekJpaRepository extends JpaRepository<Rachunek, Long> {

    @Query("""
                select distinct r from Rachunek r
                join fetch r.uzytkownik u
                left join fetch r.pozycje p
                left join fetch p.towar t
                where r.dataRozpoczecia between :start and :end
            """)
    List<Rachunek> findAllByDataZamBetweenWithUserAndPozycje(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Rachunek> findByDataRozpoczeciaBetween(LocalDateTime start, LocalDateTime end);


    Rachunek findRachunekById(Long id);

    @Query("""
                select r from Rachunek r
                join fetch r.uzytkownik u
                where r.id = :id
            """)
    Optional<Rachunek> findByIdWithUzytkownik(@Param("id") Long id);

    @Query("""
              select distinct r from Rachunek r
              join fetch r.uzytkownik u
              left join fetch r.pozycje p
              where r.id = :id
            """)
    Optional<Rachunek> findByIdWithUserAndPozycje(@Param("id") Long id);

    @Query("""
              select distinct r from Rachunek r
              join fetch r.uzytkownik u
              left join fetch r.pozycje p
              left join fetch p.towar t
              where r.id = :id
            """)
    Optional<Rachunek> findByIdWithUserPozycjeAndTowar(@Param("id") Long id);

    //    @Query("""
//              select new pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina(
//                u.id,
//                u.nazwaUzytkownika,
//                cast(function('hour', r.dataRozpoczecia) as integer),
//                coalesce(sum(r.wartNu), 0)
//              )
//              from Rachunek r join r.uzytkownik u
//              where r.dataRozpoczecia >= :from and r.dataRozpoczecia < :to
//              group by u.id, u.nazwaUzytkownika, cast(function('hour', r.dataRozpoczecia) as integer)
//              order by u.nazwaUzytkownika, cast(function('hour', r.dataRozpoczecia) as integer)
//            """)
    @Query(value = """
              SELECT
                  u.ID_UZ                                AS sellerId,
                  u.NAZWA_UZ                             AS sellerName,
                  EXTRACT(HOUR FROM r.DATA_ROZ)          AS godzina,
                  COALESCE(SUM(r.WART_NU), 0)            AS suma
              FROM RACHUNKI r
              JOIN UZYTKOWNICY u ON u.ID_UZ = r.ID_UZ
              WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
              GROUP BY u.ID_UZ, u.NAZWA_UZ, EXTRACT(HOUR FROM r.DATA_ROZ)
              ORDER BY u.NAZWA_UZ, godzina
            """, nativeQuery = true)
    List<pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina> sumyGodzinowe(LocalDateTime from, LocalDateTime to);


    @Query(value = """
            SELECT
                u.ID_UZ                                   AS sellerId,
                u.NAZWA_UZ                                AS sellerName,
                EXTRACT(WEEKDAY FROM r.DATA_ROZ)          AS dzien,
                EXTRACT(HOUR    FROM r.DATA_ROZ)          AS godzina,
                COALESCE(SUM(r.WART_NU), 0)               AS suma
            FROM RACHUNKI r
            JOIN UZYTKOWNICY u ON u.ID_UZ = r.ID_UZ
            WHERE r.DATA_ROZ >= :from
              AND r.DATA_ROZ <  :to
              AND u.ID_UZ IN (:sellerIds)
            GROUP BY u.ID_UZ, u.NAZWA_UZ,
                     EXTRACT(WEEKDAY FROM r.DATA_ROZ),
                     EXTRACT(HOUR    FROM r.DATA_ROZ)
            ORDER BY u.NAZWA_UZ, dzien, godzina
            """, nativeQuery = true)
    List<pl.kurs.sogaapplication.models.ObrotDzienGodzinaView> sumyDzienGodzina(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("sellerIds") java.util.Collection<Integer> sellerIds
    );

    @Query(value = """
            SELECT
                u.ID_UZ                                   AS sellerId,
                u.NAZWA_UZ                                AS sellerName,
                EXTRACT(WEEKDAY FROM r.DATA_ROZ)          AS dzien,
                EXTRACT(HOUR    FROM r.DATA_ROZ)          AS godzina,
                COALESCE(SUM(r.WART_NU), 0)               AS suma
            FROM RACHUNKI r
            JOIN UZYTKOWNICY u ON u.ID_UZ = r.ID_UZ
            WHERE r.DATA_ROZ >= :from
              AND r.DATA_ROZ <  :to
              AND u.ID_UZ IN (:sellerIds)
            GROUP BY u.ID_UZ, u.NAZWA_UZ,
                     EXTRACT(WEEKDAY FROM r.DATA_ROZ),
                     EXTRACT(HOUR    FROM r.DATA_ROZ)
            ORDER BY u.NAZWA_UZ, dzien, godzina
            """, nativeQuery = true)
    List<pl.kurs.sogaapplication.models.ObrotDzien> sumyDzien(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("sellerIds") java.util.Collection<Integer> sellerIds
    );

    @Query(value = """
  SELECT COALESCE(SUM(r.WART_NU), 0)
  FROM RACHUNKI r
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
  """, nativeQuery = true)
    BigDecimal sumaRazem(@Param("from") LocalDateTime from,
                         @Param("to")   LocalDateTime to);
    /**
     * Suma sprzedaży KUCHNI liczona po konkretnych towarach (ID_TW), z korektą zestawów.
     */
    @Query(value = """
  SELECT COALESCE(SUM(
           CASE WHEN p.ID_TW IN (:productIds) THEN
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)  -- dziecko * ilość rodzica
                     ELSE p.WART_NU                                     -- zwykła pozycja
                END
           ELSE 0 END
         ), 0)
  FROM RACHUNKI r
  JOIN POZRACH p        ON p.ID_RACH = r.ID_RACH
  LEFT JOIN POZRACH parent
         ON parent.ID_RACH = p.ID_RACH
        AND parent.NR_POZ  = p.NR_POZ
        AND parent.NR_POZ_KOR = 0
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
  """, nativeQuery = true)
    BigDecimal sumaKuchnia(@Param("from") LocalDateTime from,
                           @Param("to")   LocalDateTime to,
                           @Param("productIds") Collection<Long> productIds);


//    @Query(value = """
//    SELECT
//      CAST(r.DATA_ZAK AS DATE) AS dzien,
//      r.ID_UZ                  AS sellerId,
//      u.NAZWA_UZ               AS sellerName,
//      COALESCE(SUM(r.WART_NU), 0) AS suma
//    FROM RACHUNKI r
//    JOIN UZYTKOWNICY u ON u.ID_UZ = r.ID_UZ
//    WHERE r.DATA_ZAK >= :from
//      AND r.DATA_ZAK <  :to
//      AND r.ID_UZ IN (:sellerIds)
//    GROUP BY CAST(r.DATA_ZAK AS DATE), r.ID_UZ, u.NAZWA_UZ
//    ORDER BY dzien, sellerName
//    """, nativeQuery = true)
//    List<SprzedazDziennaView> sprzedazDziennaWielu(
//            @Param("from") LocalDateTime from,
//            @Param("to")   LocalDateTime to,
//            @Param("sellerIds") Collection<Integer> sellerIds
//    );

    @Query(value = """
  SELECT COALESCE(SUM(r.WART_NU), 0)
  FROM RACHUNKI r
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND r.ID_UZ IN (:sellerIds)
  """, nativeQuery = true)
    BigDecimal sumaRazemBySellers(@Param("from") LocalDateTime from,
                                  @Param("to")   LocalDateTime to,
                                  @Param("sellerIds") Collection<Integer> sellerIds);

    @Query(value = """
  SELECT COUNT(*)
  FROM RACHUNKI r
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND r.ID_UZ IN (:sellerIds)
  """, nativeQuery = true)
    long liczbaRachunkowBySellers(@Param("from") LocalDateTime from,
                                  @Param("to")   LocalDateTime to,
                                  @Param("sellerIds") Collection<Integer> sellerIds);

    // KUCHNIA po konkretnych towarach (ID_TW) z korektą zestawów + filtr po sprzedawcach
    @Query(value = """
  SELECT COALESCE(SUM(
           CASE WHEN p.ID_TW IN (:productIds) THEN
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                     ELSE p.WART_NU
                END
           ELSE 0 END
         ), 0)
  FROM RACHUNKI r
  JOIN POZRACH p        ON p.ID_RACH = r.ID_RACH
  LEFT JOIN POZRACH parent
         ON parent.ID_RACH = p.ID_RACH
        AND parent.NR_POZ  = p.NR_POZ
        AND parent.NR_POZ_KOR = 0
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND r.ID_UZ IN (:sellerIds)
  """, nativeQuery = true)
    BigDecimal sumaKuchniaBySellers(@Param("from") LocalDateTime from,
                                    @Param("to")   LocalDateTime to,
                                    @Param("productIds") Collection<Long> productIds,
                                    @Param("sellerIds") Collection<Integer> sellerIds);

    /**
     * Sumuje sprzedaż bufetu po produktach bufetowych (ID_TW) z korektą zestawów + filtr po sprzedawcach.
     */
    @Query(value = """
  SELECT COALESCE(SUM(
           CASE WHEN p.ID_TW IN (:productIds) THEN
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                     ELSE p.WART_NU
                END
           ELSE 0 END
         ), 0)
  FROM RACHUNKI r
  JOIN POZRACH p        ON p.ID_RACH = r.ID_RACH
  LEFT JOIN POZRACH parent
         ON parent.ID_RACH = p.ID_RACH
        AND parent.NR_POZ  = p.NR_POZ
        AND parent.NR_POZ_KOR = 0
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND r.ID_UZ IN (:sellerIds)
  """, nativeQuery = true)
    BigDecimal sumaBufetByProductsBySellers(@Param("from") LocalDateTime from,
                                           @Param("to")   LocalDateTime to,
                                           @Param("productIds") Collection<Long> productIds,
                                           @Param("sellerIds") Collection<Integer> sellerIds);

    /**
     * Sumuje sprzedaż bufetu po grupach bufetowych (ID_GR) z korektą zestawów (bez filtrowania po sprzedawcach).
     * Wyklucza produkty już w liście produktów bufetowych, żeby uniknąć podwójnego liczenia.
     */
    @Query(value = """
  SELECT COALESCE(SUM(
           CASE WHEN t.ID_GR IN (:groupIds) AND (:excludedProductIdsCount = 0 OR p.ID_TW NOT IN (:excludedProductIds)) THEN
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                     ELSE p.WART_NU
                END
           ELSE 0 END
         ), 0)
  FROM RACHUNKI r
  JOIN POZRACH p        ON p.ID_RACH = r.ID_RACH
  LEFT JOIN TOWARY t    ON t.ID_TW = p.ID_TW
  LEFT JOIN POZRACH parent
         ON parent.ID_RACH = p.ID_RACH
        AND parent.NR_POZ  = p.NR_POZ
        AND parent.NR_POZ_KOR = 0
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND t.ID_GR IN (:groupIds)
  """, nativeQuery = true)
    BigDecimal sumaBufetByGroups(@Param("from") LocalDateTime from,
                                 @Param("to")   LocalDateTime to,
                                 @Param("groupIds") Collection<Integer> groupIds,
                                 @Param("excludedProductIds") Collection<Long> excludedProductIds,
                                 @Param("excludedProductIdsCount") int excludedProductIdsCount);

    /**
     * Sumuje sprzedaż bufetu po grupach bufetowych (ID_GR) z korektą zestawów + filtr po sprzedawcach.
     * Wyklucza produkty już w liście produktów bufetowych, żeby uniknąć podwójnego liczenia.
     */
    @Query(value = """
  SELECT COALESCE(SUM(
           CASE WHEN t.ID_GR IN (:groupIds) AND (:excludedProductIdsCount = 0 OR p.ID_TW NOT IN (:excludedProductIds)) THEN
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                     ELSE p.WART_NU
                END
           ELSE 0 END
         ), 0)
  FROM RACHUNKI r
  JOIN POZRACH p        ON p.ID_RACH = r.ID_RACH
  LEFT JOIN TOWARY t    ON t.ID_TW = p.ID_TW
  LEFT JOIN POZRACH parent
         ON parent.ID_RACH = p.ID_RACH
        AND parent.NR_POZ  = p.NR_POZ
        AND parent.NR_POZ_KOR = 0
  WHERE r.DATA_ROZ >= :from AND r.DATA_ROZ < :to
    AND r.ID_UZ IN (:sellerIds)
    AND t.ID_GR IN (:groupIds)
  """, nativeQuery = true)
    BigDecimal sumaBufetByGroupsBySellers(@Param("from") LocalDateTime from,
                                    @Param("to")   LocalDateTime to,
                                    @Param("groupIds") Collection<Integer> groupIds,
                                         @Param("sellerIds") Collection<Integer> sellerIds,
                                         @Param("excludedProductIds") Collection<Long> excludedProductIds,
                                         @Param("excludedProductIdsCount") int excludedProductIdsCount);

    /**
     * Pobiera szczegółową listę pozycji sprzedanych dla konkretnej daty i sprzedawców.
     * Zwraca pozycje z rachunków wraz z informacją o kategorii (kuchnia/bufet/opakowania/dowóz).
     */
    @Query(value = """
        SELECT 
            r.ID_RACH as rachunekId,
            u.NAZWA_UZ as sellerName,
            u.ID_UZ as sellerId,
            t.ID_TW as towarId,
            t.NAZWA_TW as towarNazwa,
            t.ID_GR as towarGrupa,
            CASE 
                WHEN p.NR_POZ_KOR > 0 THEN COALESCE(parent.ILOSC, p.ILOSC)
                ELSE p.ILOSC
            END as ilosc,
            CASE 
                WHEN p.NR_POZ_KOR > 0 THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                ELSE p.WART_NU
            END as wartoscNetto
        FROM POZRACH p
        JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
        JOIN UZYTKOWNICY u ON u.ID_UZ = r.ID_UZ
        LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
        LEFT JOIN POZRACH parent
               ON parent.ID_RACH = p.ID_RACH
              AND parent.NR_POZ  = p.NR_POZ
              AND parent.NR_POZ_KOR = 0
        WHERE r.DATA_ROZ >= :from
          AND r.DATA_ROZ < :to
          AND r.ID_UZ IN (:sellerIds)
        ORDER BY r.ID_RACH, p.NR_POZ
        """, nativeQuery = true)
    List<Object[]> findSalesItemsByDateAndSellers(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to,
                                    @Param("sellerIds") Collection<Integer> sellerIds);

    // Diagnostyczne zapytanie do sprawdzania grup towarów używanych przez Kuchnię Domową
    @Query(value = """
            SELECT
                t.ID_GR,
                COUNT(DISTINCT p.ID_RACH),
                COUNT(DISTINCT p.ID_TW),
                COALESCE(SUM(
                    CASE WHEN p.NR_POZ_KOR > 0
                         THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                         ELSE p.WART_NU
                    END
                ), 0),
                COALESCE(SUM(p.ILOSC), 0)
            FROM POZRACH p
            JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
            LEFT JOIN POZRACH parent
                   ON parent.ID_RACH = p.ID_RACH
                  AND parent.NR_POZ  = p.NR_POZ
                  AND parent.NR_POZ_KOR = 0
            LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
            WHERE r.ID_UZ = :sellerId
              AND r.DATA_ROZ >= :from
              AND r.DATA_ROZ < :to
              AND t.ID_GR IS NOT NULL
            GROUP BY t.ID_GR
            ORDER BY COALESCE(SUM(
                CASE WHEN p.NR_POZ_KOR > 0
                     THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                     ELSE p.WART_NU
                END
            ), 0) DESC
            """, nativeQuery = true)
    List<Object[]> findProductGroupsBySeller(
            @Param("sellerId") Integer sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

}

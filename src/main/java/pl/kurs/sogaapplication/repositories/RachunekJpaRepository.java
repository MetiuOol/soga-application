package pl.kurs.sogaapplication.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.kurs.sogaapplication.models.ObrotSprzedawcyGodzina;
import pl.kurs.sogaapplication.models.Rachunek;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

}

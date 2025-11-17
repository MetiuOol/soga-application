package pl.kurs.sogaapplication.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.kurs.sogaapplication.dto.DokumentZakupuDto;
import pl.kurs.sogaapplication.models.Dokument;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Repozytorium dla dokumentów magazynowych (zakupy, PZ, FZ).
 */
public interface DokumentJpaRepository extends JpaRepository<Dokument, Long> {

    @Query(value = """
        SELECT COALESCE(SUM(d.WART_NU), 0)
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'FZ'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA IN (:warehouseIds)
        """, nativeQuery = true)
    BigDecimal sumFzNettoByWarehouses(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to,
                                      @Param("warehouseIds") Collection<Integer> warehouseIds);

    @Query(value = """
        SELECT COALESCE(SUM(p.WART_NU), 0)
        FROM DOKUMENTY p
        WHERE p.TYP_DOK = 'PZ'
          AND p.DATA_WST >= :from AND p.DATA_WST < :to
          AND p.ID_MA IN (:warehouseIds)
          AND NOT EXISTS (
                SELECT 1
                FROM DOKUMENTY f
                WHERE f.ID_POCHOD = p.ID_DOK
                  AND f.TYP_DOK = 'FZ'
            )
        """, nativeQuery = true)
    BigDecimal sumStandalonePzNettoByWarehouses(@Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                @Param("warehouseIds") Collection<Integer> warehouseIds);

    @Query(value = """
        SELECT COALESCE(SUM(d.WART_NU), 0)
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'KFZ'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA IN (:warehouseIds)
        """, nativeQuery = true)
    BigDecimal sumKfzNettoByWarehouses(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("warehouseIds") Collection<Integer> warehouseIds);

    @Query(value = """
        SELECT COALESCE(SUM(d.WART_NU), 0)
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'MMP'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA = :targetWarehouseId
          AND d.ID_MA_2 = :sourceWarehouseId
        """, nativeQuery = true)
    BigDecimal sumMmpNettoByWarehouses(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("targetWarehouseId") Integer targetWarehouseId,
                                       @Param("sourceWarehouseId") Integer sourceWarehouseId);

    @Query(value = """
        SELECT COALESCE(SUM(d.WART_NU), 0)
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'MM'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA = :sourceWarehouseId
        """, nativeQuery = true)
    BigDecimal sumMmNettoByWarehouses(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to,
                                      @Param("sourceWarehouseId") Integer sourceWarehouseId);

    /**
     * Zwraca listę dokumentów FZ z magazynów kuchni w zadanym okresie.
     */
    @Query(value = """
        SELECT d.ID_DOK, d.TYP_DOK, d.ID_POCHOD, d.NR_ORYGIN, d.ID_FI, 
               d.DATA_WST, d.CALY_NR, d.WART_NU
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'FZ'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA IN (:warehouseIds)
        ORDER BY d.DATA_WST, d.ID_DOK
        """, nativeQuery = true)
    List<Object[]> findFzDocumentsByWarehouses(@Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                @Param("warehouseIds") Collection<Integer> warehouseIds);

    /**
     * Zwraca listę dokumentów PZ z magazynów kuchni w zadanym okresie.
     * Tylko PZ z ID_POCHOD = 0 (lub NULL) - czyli te, które nie są powiązane z innymi dokumentami.
     */
    @Query(value = """
        SELECT p.ID_DOK, p.TYP_DOK, p.ID_POCHOD, p.NR_ORYGIN, p.ID_FI,
               p.DATA_WST, p.CALY_NR, p.WART_NU
        FROM DOKUMENTY p
        WHERE p.TYP_DOK = 'PZ'
          AND p.DATA_WST >= :from AND p.DATA_WST < :to
          AND p.ID_MA IN (:warehouseIds)
          AND (p.ID_POCHOD = 0 OR p.ID_POCHOD IS NULL)
        ORDER BY p.DATA_WST, p.ID_DOK
        """, nativeQuery = true)
    List<Object[]> findStandalonePzDocumentsByWarehouses(@Param("from") LocalDate from,
                                                         @Param("to") LocalDate to,
                                                         @Param("warehouseIds") Collection<Integer> warehouseIds);

    /**
     * Zwraca listę dokumentów KFZ z magazynów kuchni w zadanym okresie.
     */
    @Query(value = """
        SELECT d.ID_DOK, d.TYP_DOK, d.ID_POCHOD, d.NR_ORYGIN, d.ID_FI, 
               d.DATA_WST, d.CALY_NR, d.WART_NU
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'KFZ'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA IN (:warehouseIds)
        ORDER BY d.DATA_WST, d.ID_DOK
        """, nativeQuery = true)
    List<Object[]> findKfzDocumentsByWarehouses(@Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                @Param("warehouseIds") Collection<Integer> warehouseIds);

    /**
     * Zwraca listę dokumentów MMP (przeniesienia z bufetu do kuchni) w zadanym okresie.
     * MMP: ID_MA = 8 (kuchnia - docelowy), ID_MA_2 = 9 (bufet - źródłowy)
     */
    @Query(value = """
        SELECT d.ID_DOK, d.TYP_DOK, d.ID_POCHOD, d.NR_ORYGIN, d.ID_FI, 
               d.DATA_WST, d.CALY_NR, d.WART_NU
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'MMP'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA = :targetWarehouseId
          AND d.ID_MA_2 = :sourceWarehouseId
        ORDER BY d.DATA_WST, d.ID_DOK
        """, nativeQuery = true)
    List<Object[]> findMmpDocumentsByWarehouses(@Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                @Param("targetWarehouseId") Integer targetWarehouseId,
                                                @Param("sourceWarehouseId") Integer sourceWarehouseId);

    /**
     * Zwraca listę dokumentów MM (przeniesienia z kuchni) w zadanym okresie.
     * MM: ID_MA = 8 (kuchnia - źródłowy, skąd wychodzi), ID_MA_2 dowolny
     */
    @Query(value = """
        SELECT d.ID_DOK, d.TYP_DOK, d.ID_POCHOD, d.NR_ORYGIN, d.ID_FI, 
               d.DATA_WST, d.CALY_NR, d.WART_NU
        FROM DOKUMENTY d
        WHERE d.TYP_DOK = 'MM'
          AND d.DATA_WST >= :from AND d.DATA_WST < :to
          AND d.ID_MA = :sourceWarehouseId
        ORDER BY d.DATA_WST, d.ID_DOK
        """, nativeQuery = true)
    List<Object[]> findMmDocumentsByWarehouses(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to,
                                               @Param("sourceWarehouseId") Integer sourceWarehouseId);
}


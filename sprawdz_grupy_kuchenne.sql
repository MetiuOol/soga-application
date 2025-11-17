-- ============================================================
-- SPRAWDZENIE GRUP TOWARÓW KUCHENNYCH
-- ============================================================
-- Znajduje jakie ID_GR mają towary sprzedawane przez Kuchnię Domową
-- oraz porównuje z tym co jest sprzedawane w Ratuszowej

-- 1. GRUPY TOWARÓW SPRZEDAWANYCH PRZEZ KUCHNIĘ DOMOWĄ (ID_UZ = 11)
-- ------------------------------------------------------------
SELECT 
    t.ID_GR as id_grupy,
    COUNT(DISTINCT p.ID_RACH) as liczba_rachunkow,
    COUNT(DISTINCT p.ID_TW) as liczba_roznych_towarow,
    COALESCE(SUM(
        CASE WHEN p.NR_POZ_KOR > 0
             THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
             ELSE p.WART_NU
        END
    ), 0) as przychod_netto,
    COALESCE(SUM(p.ILOSC), 0) as ilosc_sprzedana
FROM POZRACH p
JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
LEFT JOIN POZRACH parent
       ON parent.ID_RACH = p.ID_RACH
      AND parent.NR_POZ  = p.NR_POZ
      AND parent.NR_POZ_KOR = 0
LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
WHERE r.ID_UZ = 11  -- Kuchnia Domowa
  AND r.DATA_ROZ >= '2025-01-01'
  AND r.DATA_ROZ < '2025-02-01'
  AND t.ID_GR IS NOT NULL
GROUP BY t.ID_GR
ORDER BY przychod_netto DESC;


-- 2. GRUPY TOWARÓW SPRZEDAWANYCH PRZEZ RATUSZOWĄ (wszyscy inni niż 11)
-- ------------------------------------------------------------
SELECT 
    t.ID_GR as id_grupy,
    COUNT(DISTINCT p.ID_RACH) as liczba_rachunkow,
    COUNT(DISTINCT p.ID_TW) as liczba_roznych_towarow,
    COALESCE(SUM(
        CASE WHEN p.NR_POZ_KOR > 0
             THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
             ELSE p.WART_NU
        END
    ), 0) as przychod_netto,
    COALESCE(SUM(p.ILOSC), 0) as ilosc_sprzedana
FROM POZRACH p
JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
LEFT JOIN POZRACH parent
       ON parent.ID_RACH = p.ID_RACH
      AND parent.NR_POZ  = p.NR_POZ
      AND parent.NR_POZ_KOR = 0
LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
WHERE r.ID_UZ != 11  -- Ratuszowa (wszyscy inni)
  AND r.DATA_ROZ >= '2025-01-01'
  AND r.DATA_ROZ < '2025-02-01'
  AND t.ID_GR IS NOT NULL
GROUP BY t.ID_GR
ORDER BY przychod_netto DESC;


-- 3. PORÓWNANIE: które grupy są wspólne, które tylko w KD, które tylko w Ratuszowej
-- ------------------------------------------------------------
SELECT 
    COALESCE(kd.ID_GR, ratusz.ID_GR) as id_grupy,
    CASE WHEN kd.ID_GR IS NOT NULL THEN 'TAK' ELSE 'NIE' END as uzywane_w_KD,
    CASE WHEN ratusz.ID_GR IS NOT NULL THEN 'TAK' ELSE 'NIE' END as uzywane_w_Ratuszowej,
    COALESCE(kd.przychod_netto, 0) as przychod_KD,
    COALESCE(ratusz.przychod_netto, 0) as przychod_Ratuszowa,
    CASE 
        WHEN kd.ID_GR IS NOT NULL AND ratusz.ID_GR IS NULL THEN 'TYLKO KD'
        WHEN kd.ID_GR IS NULL AND ratusz.ID_GR IS NOT NULL THEN 'TYLKO RATUSZOWA'
        ELSE 'WSPÓLNE'
    END as typ
FROM (
    SELECT 
        t.ID_GR,
        COALESCE(SUM(
            CASE WHEN p.NR_POZ_KOR > 0
                 THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                 ELSE p.WART_NU
            END
        ), 0) as przychod_netto
    FROM POZRACH p
    JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
    LEFT JOIN POZRACH parent
           ON parent.ID_RACH = p.ID_RACH
          AND parent.NR_POZ  = p.NR_POZ
          AND parent.NR_POZ_KOR = 0
    LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
    WHERE r.ID_UZ = 11
      AND r.DATA_ROZ >= '2025-01-01'
      AND r.DATA_ROZ < '2025-02-01'
      AND t.ID_GR IS NOT NULL
    GROUP BY t.ID_GR
) kd
FULL OUTER JOIN (
    SELECT 
        t.ID_GR,
        COALESCE(SUM(
            CASE WHEN p.NR_POZ_KOR > 0
                 THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)
                 ELSE p.WART_NU
            END
        ), 0) as przychod_netto
    FROM POZRACH p
    JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
    LEFT JOIN POZRACH parent
           ON parent.ID_RACH = p.ID_RACH
          AND parent.NR_POZ  = p.NR_POZ
          AND parent.NR_POZ_KOR = 0
    LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
    WHERE r.ID_UZ != 11
      AND r.DATA_ROZ >= '2025-01-01'
      AND r.DATA_ROZ < '2025-02-01'
      AND t.ID_GR IS NOT NULL
    GROUP BY t.ID_GR
) ratusz ON kd.ID_GR = ratusz.ID_GR
ORDER BY 
    CASE 
        WHEN kd.ID_GR IS NOT NULL AND ratusz.ID_GR IS NULL THEN 1
        WHEN kd.ID_GR IS NOT NULL AND ratusz.ID_GR IS NOT NULL THEN 2
        ELSE 3
    END,
    COALESCE(kd.przychod_netto, ratusz.przychod_netto) DESC;





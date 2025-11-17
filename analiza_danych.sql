-- ============================================================
-- ANALIZA DANYCH - SPRAWDZENIE STRUKTURY BIZNESOWEJ
-- ============================================================

-- 1. ANALIZA GODZIN RUCHU - sprawdzenie godzin sprzedaży dla obu punktów
-- ------------------------------------------------------------
-- A) Kuchnia Domowa (ID_UZ = 11) - godziny pracy: 11-18 pn-pt, 11-14 sobota
SELECT 
    EXTRACT(WEEKDAY FROM r.DATA_ROZ) as dzien_tygodnia,  -- 0=Niedziela, 1=Poniedziałek, ...
    EXTRACT(HOUR FROM r.DATA_ROZ) as godzina,
    COUNT(*) as liczba_rachunkow,
    COALESCE(SUM(r.WART_NU), 0) as przychod_netto
FROM RACHUNKI r
WHERE r.ID_UZ = 11  -- Kuchnia Domowa
  AND r.DATA_ROZ >= '2025-01-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY EXTRACT(WEEKDAY FROM r.DATA_ROZ), EXTRACT(HOUR FROM r.DATA_ROZ)
ORDER BY dzien_tygodnia, godzina;

-- B) Ratuszowa (wszyscy inni sprzedawcy) - godziny pracy: 12-21 codziennie
SELECT 
    EXTRACT(WEEKDAY FROM r.DATA_ROZ) as dzien_tygodnia,
    EXTRACT(HOUR FROM r.DATA_ROZ) as godzina,
    COUNT(*) as liczba_rachunkow,
    COALESCE(SUM(r.WART_NU), 0) as przychod_netto
FROM RACHUNKI r
WHERE r.ID_UZ != 11  -- Ratuszowa (wszyscy inni)
  AND r.DATA_ROZ >= '2025-01-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY EXTRACT(WEEKDAY FROM r.DATA_ROZ), EXTRACT(HOUR FROM r.DATA_ROZ)
ORDER BY dzien_tygodnia, godzina;


-- 2. NAJLEPIEJ SPRZEDAJĄCE SIĘ PRODUKTY/TOWARY
-- ------------------------------------------------------------
SELECT 
    t.ID_TW as id_towaru,
    t.NAZWA_TW as nazwa_towaru,
    t.ID_GR as id_grupy,
    COUNT(DISTINCT p.ID_RACH) as liczba_sprzedazy,
    COALESCE(SUM(
        CASE WHEN p.NR_POZ_KOR > 0
             THEN p.WART_JN * COALESCE(parent.ILOSC, p.ILOSC)  -- dziecko * ilość rodzica
             ELSE p.WART_NU                                     -- zwykła pozycja
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
WHERE r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY t.ID_TW, t.NAZWA_TW, t.ID_GR
ORDER BY przychod_netto DESC
FIRST 50;


-- 3. ANALIZA PODZIAŁU SPRZEDAŻY: KD vs RATUSZOWA (proporcje dla alokacji kosztów)
-- ------------------------------------------------------------
SELECT 
    CAST(r.DATA_ROZ AS DATE) as dzien,
    CASE WHEN r.ID_UZ = 11 THEN 'Kuchnia Domowa' ELSE 'Ratuszowa' END as punkt,
    COUNT(*) as liczba_rachunkow,
    COALESCE(SUM(r.WART_NU), 0) as przychod_netto,
    EXTRACT(WEEKDAY FROM r.DATA_ROZ) as dzien_tygodnia
FROM RACHUNKI r
WHERE r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY CAST(r.DATA_ROZ AS DATE), 
         CASE WHEN r.ID_UZ = 11 THEN 'Kuchnia Domowa' ELSE 'Ratuszowa' END,
         EXTRACT(WEEKDAY FROM r.DATA_ROZ)
ORDER BY dzien, punkt;


-- 4. SPRZEDAŻ PO GODZINACH - porównanie KD vs RATUSZOWA w tych samych godzinach
-- ------------------------------------------------------------
SELECT 
    EXTRACT(HOUR FROM r.DATA_ROZ) as godzina,
    CASE WHEN r.ID_UZ = 11 THEN 'Kuchnia Domowa' ELSE 'Ratuszowa' END as punkt,
    COUNT(*) as liczba_rachunkow,
    COALESCE(SUM(r.WART_NU), 0) as przychod_netto
FROM RACHUNKI r
WHERE r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY EXTRACT(HOUR FROM r.DATA_ROZ),
         CASE WHEN r.ID_UZ = 11 THEN 'Kuchnia Domowa' ELSE 'Ratuszowa' END
ORDER BY godzina, punkt;


-- 5. ANALIZA DZIENNEGO ZAROBKU - podsumowanie dzienne
-- ------------------------------------------------------------
SELECT 
    CAST(r.DATA_ROZ AS DATE) as dzien,
    COUNT(DISTINCT r.ID_RACH) as liczba_rachunkow,
    COALESCE(SUM(r.WART_NU), 0) as przychod_netto_dzien,
    COUNT(DISTINCT CASE WHEN r.ID_UZ = 11 THEN r.ID_RACH END) as rachunki_KD,
    COALESCE(SUM(CASE WHEN r.ID_UZ = 11 THEN r.WART_NU ELSE 0 END), 0) as przychod_KD,
    COUNT(DISTINCT CASE WHEN r.ID_UZ != 11 THEN r.ID_RACH END) as rachunki_Ratuszowa,
    COALESCE(SUM(CASE WHEN r.ID_UZ != 11 THEN r.WART_NU ELSE 0 END), 0) as przychod_Ratuszowa,
    EXTRACT(WEEKDAY FROM r.DATA_ROZ) as dzien_tygodnia
FROM RACHUNKI r
WHERE r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY CAST(r.DATA_ROZ AS DATE), EXTRACT(WEEKDAY FROM r.DATA_ROZ)
ORDER BY dzien;


-- 6. ANALIZA TYPÓW SPRZEDAŻY KUCHNI DOMOWEJ (według TypSprzedazyKD)
-- ------------------------------------------------------------
-- Sprawdzenie czy produkty z TypSprzedazyKD są faktycznie w sprzedaży
SELECT 
    CASE 
        WHEN t.ID_TW IN (4469, 4593) THEN 'NA_WAGE'
        WHEN t.ID_TW = 4468 THEN 'ZUPA'
        WHEN t.ID_TW = 4794 THEN 'ABONAMENT'
        WHEN t.ID_TW = 4472 THEN 'KARNET_ZWYKLY'
        WHEN t.ID_TW = 5157 THEN 'KARNET_MIESNY'
        WHEN t.ID_TW IN (951, 6438) THEN 'OPAKOWANIE'
        ELSE 'INNE'
    END as typ_sprzedazy,
    COUNT(DISTINCT p.ID_RACH) as liczba_sprzedazy,
    COALESCE(SUM(p.WART_NU), 0) as przychod_netto,
    COALESCE(SUM(p.ILOSC), 0) as ilosc_sprzedana
FROM POZRACH p
JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
LEFT JOIN TOWARY t ON t.ID_TW = p.ID_TW
WHERE r.ID_UZ = 11  -- Tylko Kuchnia Domowa
  AND r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01'
GROUP BY 
    CASE 
        WHEN t.ID_TW IN (4469, 4593) THEN 'NA_WAGE'
        WHEN t.ID_TW = 4468 THEN 'ZUPA'
        WHEN t.ID_TW = 4794 THEN 'ABONAMENT'
        WHEN t.ID_TW = 4472 THEN 'KARNET_ZWYKLY'
        WHEN t.ID_TW = 5157 THEN 'KARNET_MIESNY'
        WHEN t.ID_TW IN (951, 6438) THEN 'OPAKOWANIE'
        ELSE 'INNE'
    END
ORDER BY przychod_netto DESC;


-- 7. SPRAWDZENIE STRUKTURY POZRACH (dla zrozumienia korekt i zestawów)
-- ------------------------------------------------------------
-- Sprawdzenie ile pozycji ma korekty (NR_POZ_KOR > 0)
SELECT 
    COUNT(*) as liczba_pozycji,
    COUNT(CASE WHEN p.NR_POZ_KOR > 0 THEN 1 END) as pozycje_z_korekta,
    COUNT(CASE WHEN p.NR_POZ_KOR = 0 THEN 1 END) as pozycje_zwykle
FROM POZRACH p
JOIN RACHUNKI r ON r.ID_RACH = p.ID_RACH
WHERE r.DATA_ROZ >= '2025-07-01'
  AND r.DATA_ROZ < '2025-08-01';






# 📊 KIERUNEK ROZWOJU APLIKACJI - PLAN DZIAŁANIA

## 📋 OBECNA SYTUACJA

### Model biznesowy:
- **Jeden biznes, jeden magazyn** - wspólna kuchnia dla obu punktów
- **Kuchnia Domowa (KD)**: 11-18 pn-pt, 11-14 sobota (sprzedaż na wagę 59.9 zł/kg)
- **Ratuszowa**: 12-21 codziennie (restauracja vs bufet)
- **Problem**: Jak alokować koszty surowców między KD a Ratuszową?

### Obecne funkcjonalności:
✅ Analiza sprzedaży (restauracja vs bufet, dzienna, godzinowa)  
✅ Walidacja podejrzanych rachunków  
✅ Eksport raportów (XML, CSV)  
✅ Koncept analizy Kuchni Domowej (szkic)  
⚠️ Encje Dokument i Magazyn (utworzone, ale nieużywane)  

### Priorytety użytkownika:
1. ✅ **Sprawdzenie godzin ruchu** - już częściowo wdrożone
2. ❌ **Co się najlepiej sprzedaje** - do zrobienia
3. ✅ **Ile dziennie zarabiają** - już częściowo wdrożone
4. ❌ **Analiza modelu biznesowego** - do zrobienia

---

## 🎯 PROPOZOWANY PLAN DZIAŁANIA

### **FAZA 1: Analiza sprzedaży i produktów (2-3 tygodnie)**

#### 1.1 Analiza najlepiej sprzedających się produktów
**Cel**: Zrozumieć co się najlepiej sprzedaje w obu punktach

- [ ] **Repository dla analizy produktów** (`ProductSalesRepository`)
  - Zapytanie SQL: Top 50 produktów po przychodzie
  - Zapytanie SQL: Produkty po liczbie sprzedaży
  - Zapytanie SQL: Produkty po ilości sprzedanej

- [ ] **Serwis analizy produktów** (`ProductAnalysisService`)
  - `getTopProductsByRevenue()` - top produkty po przychodzie
  - `getTopProductsByQuantity()` - top produkty po ilości
  - `getProductSalesByCategory()` - sprzedaż po kategoriach (kuchnia/bufet)

- [ ] **DTO dla analizy produktów** (`ProductSalesDto`)
  - ID produktu, nazwa, grupa
  - Liczba sprzedaży
  - Przychód netto
  - Ilość sprzedana

- [ ] **Raportowanie produktów** (`ProductReportFormatter`)
  - Tabela top produktów
  - Wykres sprzedaży (tekstowy w konsoli)
  - Analiza trendów (wzrost/spadek)

#### 1.2 Rozszerzona analiza godzin ruchu
**Cel**: Lepsze zrozumienie godzin pracy i ruchu

- [ ] **Analiza godzin ruchu per punkt sprzedaży**
  - Godziny ruchu KD vs Ratuszowa
  - Godziny szczytowe dla każdego punktu
  - Analiza dni tygodnia (pn-pt vs sobota-niedziela)

- [ ] **Identyfikacja godzin optymalnych**
  - Najbardziej dochodowe godziny
  - Najmniej dochodowe godziny (możliwość optymalizacji)

---

### **FAZA 2: Dokończenie analizy Kuchni Domowej (2-3 tygodnie)**

#### 2.1 Implementacja analizy KD
**Cel**: Pełna analiza sprzedaży Kuchni Domowej

- [ ] **Dokończenie `KuchniaDomowaSalesService`**
  - Implementacja zapytań SQL dla każdego typu sprzedaży:
    - Sprzedaż na wagę (na miejscu vs na wynos)
    - Zupy
    - Abonamenty
    - Karnety zwykłe i mięsne

- [ ] **Analiza sprzedaży na wagę**
  - Ilość kg sprzedana (na miejscu vs na wynos)
  - Przychód netto z każdego typu
  - Średnia cena za kg
  - Procent sprzedaży na wynos

- [ ] **Formatowanie raportów KD** (`KuchniaDomowaReportFormatter`)
  - Tabela szczegółów sprzedaży
  - Statystyki (średnia kg/rachunek, % na wynos, cena/kg)

- [ ] **Integracja z CLI**
  - Dodanie opcji menu: "Analiza Kuchni Domowej"
  - Wyświetlanie raportów KD

---

### **FAZA 3: Alokacja kosztów (3-4 tygodnie)**

#### 3.1 Model alokacji kosztów
**Cel**: Rozwiązać problem podziału kosztów między KD a Ratuszową

**Propozycja rozwiązania**:
1. **Koszty proporcjonalne do sprzedaży** (procent przychodu)
2. **Koszty proporcjonalne do godzin pracy** (godziny pracy * stawka)
3. **Hybrydowy model** (część kosztów proporcjonalnie, część godzinowo)

#### 3.2 Implementacja alokacji kosztów

- [ ] **Encja Pracownik** (jeśli potrzebna)
  - ID, imię, nazwisko
  - Stawka godzinowa
  - Godziny pracy (może być w osobnej tabeli)

- [ ] **Serwis alokacji kosztów** (`CostAllocationService`)
  - `allocateCostsBySales()` - alokacja proporcjonalnie do sprzedaży
  - `allocateCostsByHours()` - alokacja proporcjonalnie do godzin
  - `calculateDailyCosts()` - dzienne koszty dla każdego punktu

- [ ] **Integracja z dokumentami**
  - Pobieranie kosztów z tabeli `DOKUMENTY` (FZ)
  - Kategoryzacja kosztów (surowce, pracownicy, operacyjne)

- [ ] **Raport kosztów** (`CostReportDto`)
  - Koszty dzienne/miesięczne
  - Alokacja KD vs Ratuszowa
  - Wskaźniki (koszty/przychody, marża)

---

### **FAZA 4: Analiza rentowności (2-3 tygodnie)**

#### 4.1 Kalkulacja zysków dziennych
**Cel**: Ile dziennie zarabiamy (przychody - koszty)

- [ ] **Serwis rentowności** (`ProfitabilityAnalysisService`)
  - `calculateDailyProfit()` - zysk dzienny
  - `calculateProfitMargin()` - marża zysku (%)
  - `compareProfitability()` - porównanie okresów

- [ ] **Raport rentowności** (`ProfitabilityReportDto`)
  - Przychody dzienne
  - Koszty dzienne
  - Zysk dzienny
  - Marża zysku

- [ ] **Wskaźniki efektywności**
  - Koszty surowców / Przychód (%)
  - Marża brutto
  - Zysk netto dzienny/miesięczny

---

### **FAZA 5: Analiza modelu biznesowego (2-3 tygodnie)**

#### 5.1 Porównania i trendy
**Cel**: Sprawdzenie czy można zmienić model biznesowy

- [ ] **Porównanie okresów**
  - Ten sam dzień tygodnia (miesiąc do miesiąca)
  - Tydzień do tygodnia
  - Miesiąc do miesiąca

- [ ] **Trendy sprzedaży**
  - Wzrost/spadek przychodów
  - Trendy godzinowe
  - Trendy produktowe

- [ ] **Analiza sezonowości**
  - Które dni tygodnia są najlepsze?
  - Które miesiące są najlepsze?
  - Czy są sezony wzmożonej sprzedaży?

#### 5.2 Rekomendacje biznesowe
**Cel**: Sugestie optymalizacji modelu biznesowego

- [ ] **Identyfikacja możliwości optymalizacji**
  - Najbardziej dochodowe godziny/dni
  - Najmniej dochodowe godziny/dni (możliwość zamknięcia?)
  - Produkty do promocji/usunięcia

- [ ] **Raport rekomendacji** (`BusinessRecommendationsDto`)
  - Sugestie zmian godzin pracy
  - Sugestie zmian asortymentu
  - Analiza ROI (return on investment)

---

## 📊 PRIORYTETY IMPLEMENTACJI

### **PRIORYTET 1** (Natychmiastowe):
1. ✅ Analiza godzin ruchu - rozszerzenie obecnej funkcjonalności
2. ❌ Analiza najlepiej sprzedających się produktów - NOWA FUNKCJONALNOŚĆ
3. ❌ Dokończenie analizy KD - dokończenie szkicu

### **PRIORYTET 2** (Krótkoterminowe - 1-2 miesiące):
4. ❌ Alokacja kosztów - rozwiązanie problemu podziału kosztów
5. ❌ Analiza rentowności - przychody vs koszty

### **PRIORYTET 3** (Długoterminowe - 3-6 miesięcy):
6. ❌ Analiza modelu biznesowego - trendy i rekomendacje
7. ❌ Zaawansowane raportowanie - dashboard, PDF, etc.

---

## 🚀 NASTĘPNY KROK - CO ZACZĄĆ?

**Sugerowany pierwszy krok: Analiza najlepiej sprzedających się produktów**

**Dlaczego?**
1. ✅ Nie wymaga rozwiązania problemu alokacji kosztów
2. ✅ Natychmiastowa wartość biznesowa
3. ✅ Relatywnie prosta implementacja (zapytania SQL)
4. ✅ Odpowiada na priorytet użytkownika: "co się najlepiej sprzedaje"

**Co zrobić?**
1. Stworzyć zapytania SQL do analizy produktów (już w `analiza_danych.sql`)
2. Stworzyć Repository dla produktów
3. Stworzyć Serwis analizy produktów
4. Stworzyć DTO i formatowanie raportów
5. Dodać do CLI

---

## ❓ PYTANIA DO USTALENIA

1. **Model pracowników**:
   - Czy w systemie są dane o pracownikach i ich godzinach pracy?
   - Czy potrzebujemy nową encję `Pracownik`?

2. **Alokacja kosztów**:
   - Czy preferujesz proporcjonalnie do sprzedaży czy do godzin?
   - Czy hybrydowy model (część proporcjonalnie, część godzinowo)?

3. **Koszty operacyjne**:
   - Czy koszty operacyjne (czynsz, media) mają być w systemie?
   - Czy tylko koszty surowców z tabeli `DOKUMENTY`?

4. **Priorytet**:
   - Czy zaczynamy od analizy produktów (PRIORYTET 1)?
   - Czy najpierw dokończymy analizę KD?
   - Czy zaczynamy od alokacji kosztów?

---

## 📝 UWAGI

- Zapytania SQL do analizy danych są w pliku `analiza_danych.sql`
- Koncept analizy KD jest w pakiecie `concept`
- Encje `Dokument` i `Magazyn` są gotowe do użycia
- Problem z łączeniem FZ z PZ można pominąć na razie (użyć tylko FZ dla kosztów)






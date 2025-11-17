# 🧪 JAK TESTOWAĆ APLIKACJĘ

## 📋 Spis Treści
1. [Uruchomienie aplikacji](#uruchomienie-aplikacji)
2. [Tryb CLI (Command Line Interface)](#tryb-cli)
3. [Testowanie funkcjonalności](#testowanie-funkcjonalności)
4. [Przykładowe scenariusze testowe](#przykładowe-scenariusze-testowe)
5. [Debugowanie i logi](#debugowanie-i-logi)

---

## 🚀 Uruchomienie aplikacji

### 1. Kompilacja i uruchomienie

```bash
# Kompilacja projektu
mvn clean compile

# Uruchomienie aplikacji w trybie CLI
mvn spring-boot:run -Dspring-boot.run.arguments=--cli

# Lub bezpośrednio:
java -jar target/soga-application.jar --cli
```

### 2. Tryb demo (bez argumentów)

```bash
# Uruchomienie w trybie demo (przykładowe analizy)
mvn spring-boot:run
```

---

## 💻 Tryb CLI

### Menu główne

Po uruchomieniu aplikacji z argumentem `--cli`, zobaczysz menu:

```
🍽️  WITAJ W SYSTEMIE ANALIZY RESTAURACJI! 🍽️
==================================================

📋 GŁÓWNE MENU:
1. 📊 Generuj raport sprzedaży
2. ⏰ Analiza sprzedaży godzinowej
3. 📅 Podsumowanie roczne
4. 📤 Eksport raportów
5. 🔍 Walidacja podejrzanych rachunków
6. ⚙️  Konfiguracja
7. 🏪 Punkty sprzedaży
8. 🚪 Wyjście
```

---

## 🧪 Testowanie funkcjonalności

### 1. Testowanie punktów sprzedaży

**Opcja 7: Punkty sprzedaży**

1. Uruchom aplikację: `mvn spring-boot:run -Dspring-boot.run.arguments=--cli`
2. Wybierz opcję **7** (Punkty sprzedaży)
3. Sprawdź:
   - ✅ Czy wyświetlają się oba punkty (KD i Ratuszowa)
   - ✅ Czy godziny pracy są poprawne:
     - **KD**: 11-18 pn-pt, 11-14 sobota
     - **Ratuszowa**: 12-21 codziennie
   - ✅ Czy sprzedawcy są przypisani poprawnie
   - ✅ Czy kategorie sprzedaży są wyświetlone

**Oczekiwany wynik:**
```
🏪 PUNKTY SPRZEDAŻY
----------------------------------------

📍 Kuchnia Domowa (ID: KD)
   ID Użytkownika: 11
   Sprzedawcy: [11]
   Godziny pracy:
      Poniedziałek: 11:00 - 18:00 (7h)
      Wtorek: 11:00 - 18:00 (7h)
      ...
      Sobota: 11:00 - 14:00 (3h)
   Kategorie sprzedaży: [WEIGHT_BASED, SOUPS, SUBSCRIPTIONS, ...]

📍 Ratuszowa (ID: RATUSZOWA)
   ID Użytkownika: Brak (wielu sprzedawców)
   Sprzedawcy: [1, 2, 3, 4, 5, 6, 8, 9, 12, 13, 14, 15, 16, 17]
   Godziny pracy:
      Poniedziałek: 12:00 - 21:00 (9h)
      ...
   Kategorie sprzedaży: [RESTAURANT_KITCHEN, RESTAURANT_BUFFET]
```

---

### 2. Testowanie raportu sprzedaży

**Opcja 1: Generuj raport sprzedaży**

1. Wybierz opcję **1**
2. Podaj datę początkową (np. `2025-07-01`)
3. Podaj datę końcową (np. `2025-07-31`)
4. Wybierz sprzedawców:
   - **1** - Domyślni (sprzedawca 11 - KD)
   - **2** - Wszyscy
   - **3** - Własny wybór (np. `11` lub `1,2,3`)

**Sprawdź:**
- ✅ Czy raport wyświetla się poprawnie
- ✅ Czy dane dzienne są wyświetlone z dniem tygodnia
- ✅ Czy sumy są obliczone poprawnie
- ✅ Czy statystyki są poprawne

**Przykładowe dane testowe:**
```
Data początkowa (YYYY-MM-DD): 2025-07-01
Data końcowa (YYYY-MM-DD): 2025-07-31
Wybierz sprzedawców: 1 (Domyślni - KD)
```

---

### 3. Testowanie analizy godzinowej

**Opcja 2: Analiza sprzedaży godzinowej**

1. Wybierz opcję **2**
2. Podaj datę (np. `2025-07-15`)

**Sprawdź:**
- ✅ Czy analiza godzinowa wyświetla się poprawnie
- ✅ Czy godziny są zgodne z godzinami pracy punktów
- ✅ Czy dane są pogrupowane po sprzedawcach

---

### 4. Testowanie walidacji rachunków

**Opcja 5: Walidacja podejrzanych rachunków**

1. Wybierz opcję **5**
2. Podaj zakres dat (np. `2025-07-01` do `2025-07-31`)

**Sprawdź:**
- ✅ Czy podejrzane rachunki są wykrywane
- ✅ Czy statystyki są wyświetlone
- ✅ Czy szczegóły rachunków zawierają datę i godzinę

---

### 5. Testowanie konfiguracji

**Opcja 6: Konfiguracja**

1. Wybierz opcję **6**

**Sprawdź:**
- ✅ Czy grupy towarów kuchennych są wyświetlone
- ✅ Czy lista sprzedawców jest poprawna

---

## 📝 Przykładowe scenariusze testowe

### Scenariusz 1: Raport dla Kuchni Domowej

```
1. Uruchom aplikację z --cli
2. Wybierz opcję 1 (Generuj raport sprzedaży)
3. Wprowadź:
   - Data początkowa: 2025-07-01
   - Data końcowa: 2025-07-31
   - Sprzedawcy: 1 (Domyślni - KD)
4. Sprawdź czy raport zawiera:
   - Sprzedaż tylko dla sprzedawcy 11 (KD)
   - Dane dzienne z dniem tygodnia
   - Sumy dla kuchnia/bufet/razem
```

### Scenariusz 2: Raport dla Ratuszowej

```
1. Wybierz opcję 1
2. Wprowadź:
   - Data początkowa: 2025-07-01
   - Data końcowa: 2025-07-31
   - Sprzedawcy: 2 (Wszyscy)
3. Sprawdź czy raport zawiera:
   - Sprzedaż dla wszystkich sprzedawców oprócz 11
   - Podział na kuchnię i bufet
```

### Scenariusz 3: Sprawdzenie punktów sprzedaży

```
1. Wybierz opcję 7 (Punkty sprzedaży)
2. Sprawdź:
   - Czy KD ma poprawne godziny (11-18 pn-pt, 11-14 sobota)
   - Czy Ratuszowa ma poprawne godziny (12-21 codziennie)
   - Czy kategorie sprzedaży są poprawne
```

### Scenariusz 4: Test alokacji kosztów (futurowe)

```
// Po implementacji funkcjonalności alokacji kosztów:
1. Utwórz test z kosztami:
   - Koszt całkowity: 1000 zł
   - Sprzedaż KD: 600 zł
   - Sprzedaż Ratuszowa: 400 zł
2. Sprawdź czy alokacja jest proporcjonalna:
   - KD: 600 zł
   - Ratuszowa: 400 zł
```

---

## 🔍 Debugowanie i logi

### Włączenie logów SQL

W pliku `application.properties`:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Sprawdzanie logów

Aplikacja loguje informacje na poziomie INFO i DEBUG:

```java
// W kodzie użyto:
logger.info("Generowanie raportu sprzedaży od {} do {} dla sprzedawców: {}", from, to, sellerIds);
logger.debug("Analiza sprzedaży dziennej dla miesiąca {} dla sprzedawców: {}", firstDayOfMonth, sellerIds);
```

### Najczęstsze problemy

1. **Błąd połączenia z bazą danych**
   - Sprawdź ścieżkę do bazy w `application.properties`
   - Sprawdź czy baza istnieje: `C:/bazy/gastro.fdb`

2. **Błąd kompilacji**
   - Uruchom: `mvn clean compile`
   - Sprawdź czy wszystkie zależności są dostępne

3. **Brak danych w raporcie**
   - Sprawdź czy w bazie są dane dla wybranego okresu
   - Sprawdź czy wybrano poprawnych sprzedawców

---

## 📊 Testowanie z bazą danych

### Sprawdzenie danych w bazie

Możesz użyć zapytań SQL z pliku `analiza_danych.sql` do weryfikacji danych:

```sql
-- Sprawdź czy są dane dla KD (sprzedawca 11)
SELECT COUNT(*) FROM RACHUNKI 
WHERE ID_UZ = 11 
  AND DATA_ROZ >= '2025-07-01' 
  AND DATA_ROZ < '2025-08-01';

-- Sprawdź sprzedaż godzinową
SELECT EXTRACT(HOUR FROM DATA_ROZ) as godzina, 
       SUM(WART_NU) as suma
FROM RACHUNKI
WHERE DATA_ROZ >= '2025-07-01' 
  AND DATA_ROZ < '2025-08-01'
GROUP BY EXTRACT(HOUR FROM DATA_ROZ)
ORDER BY godzina;
```

---

## ✅ Checklist testowania

- [ ] Aplikacja uruchamia się bez błędów
- [ ] Menu CLI wyświetla się poprawnie
- [ ] Opcja 7 (Punkty sprzedaży) działa
- [ ] Raport sprzedaży (opcja 1) generuje się poprawnie
- [ ] Analiza godzinowa (opcja 2) działa
- [ ] Walidacja rachunków (opcja 5) działa
- [ ] Konfiguracja (opcja 6) wyświetla poprawnie
- [ ] Godziny pracy punktów są poprawne
- [ ] Kategorie sprzedaży są poprawne
- [ ] Sprzedawcy są przypisani do właściwych punktów

---

## 🎯 Następne kroki

Po przetestowaniu podstawowej funkcjonalności:

1. **Testowanie alokacji kosztów** (po implementacji)
2. **Testowanie analizy produktów** (po implementacji)
3. **Testowanie analizy KD** (po dokończeniu implementacji)

---

## 📞 Pomoc

Jeśli napotkasz problemy:

1. Sprawdź logi aplikacji
2. Sprawdź czy baza danych jest dostępna
3. Sprawdź konfigurację w `application.properties`
4. Upewnij się że wybrane daty mają dane w bazie






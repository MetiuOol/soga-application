# 🚀 JAK URUCHOMIĆ APLIKACJĘ

## 📋 Szybki Start

### Opcja 1: Uruchomienie w trybie DEMO (bez argumentów)

Po prostu uruchom metodę `main` w klasie `SogaApplication` - aplikacja automatycznie pokaże:

1. ✅ **Punkty sprzedaży** (KD i Ratuszowa) z godzinami pracy
2. ✅ **Konfigurację** (grupy towarów, sprzedawcy)
3. ✅ **Przykładowy raport** dla wszystkich sprzedawców (styczeń 2025)
4. ✅ **Przykładowy raport** tylko dla KD (styczeń 2025)
5. ✅ **Analizę godzinową** (15 stycznia 2025)
6. ✅ **Walidację podejrzanych rachunków** (styczeń 2025)

### Opcja 2: Uruchomienie w trybie CLI (interaktywny)

Uruchom z argumentem `--cli` aby otworzyć interfejs użytkownika z menu.

---

## 💻 Jak uruchomić w IntelliJ IDEA

### Metoda 1: Uruchomienie bezpośrednio z IDE

1. Otwórz plik `SogaApplication.java`
2. Kliknij prawym przyciskiem na metodę `main`
3. Wybierz **"Run 'SogaApplication.main()'"**
4. Aplikacja uruchomi się w trybie DEMO i pokaże wszystkie funkcjonalności

### Metoda 2: Uruchomienie z argumentem --cli

1. Kliknij prawym przyciskiem na klasę `SogaApplication`
2. Wybierz **"Run 'SogaApplication'"** → **"Edit Configurations..."**
3. W polu **"Program arguments"** wpisz: `--cli`
4. Kliknij **"OK"** i uruchom aplikację
5. Zobaczysz interaktywne menu CLI

---

## 🖥️ Jak uruchomić z terminala

### Tryb DEMO (bez argumentów):
```bash
cd c:\Users\MATEUSZ\IdeaProjects\soga-application
mvn spring-boot:run
```

### Tryb CLI (z argumentem):
```bash
cd c:\Users\MATEUSZ\IdeaProjects\soga-application
mvn spring-boot:run -Dspring-boot.run.arguments=--cli
```

---

## 📊 Co zobaczysz w trybie DEMO

Aplikacja automatycznie wyświetli:

### 1. Punkty Sprzedaży
```
🏪 PUNKTY SPRZEDAŻY
============================================================
📍 Kuchnia Domowa (ID: KD)
   ID Użytkownika: 11
   Sprzedawcy: [11]
   Godziny pracy:
      Poniedziałek: 11:00 - 18:00 (7h)
      ...
   Kategorie sprzedaży: [WEIGHT_BASED, SOUPS, ...]

📍 Ratuszowa (ID: RATUSZOWA)
   ...
```

### 2. Konfiguracja
```
⚙️  KONFIGURACJA
============================================================
🏠 Grupy towarów kuchennych: [39, 40, 41, ...]
👥 Domyślni sprzedawcy: [11]
👥 Wszyscy sprzedawcy: [1, 2, 3, ...]
```

### 3. Raporty sprzedaży
- Raport dla wszystkich sprzedawców
- Raport tylko dla KD
- Analiza godzinowa
- Walidacja podejrzanych rachunków

---

## ✅ Sprawdzenie czy działa

Po uruchomieniu sprawdź:

1. ✅ Czy aplikacja uruchomiła się bez błędów
2. ✅ Czy punkty sprzedaży są wyświetlone poprawnie
3. ✅ Czy godziny pracy są poprawne:
   - **KD**: 11-18 pn-pt, 11-14 sobota
   - **Ratuszowa**: 12-21 codziennie
4. ✅ Czy raporty są generowane (jeśli są dane w bazie)
5. ✅ Czy nie ma błędów w konsoli

---

## 🔧 Rozwiązywanie problemów

### Problem: Błąd połączenia z bazą danych
**Rozwiązanie**: Sprawdź czy baza istnieje w `C:/bazy/gastro.fdb`

### Problem: Brak danych w raportach
**Rozwiązanie**: To normalne jeśli w bazie nie ma danych dla stycznia 2025. Zmień daty w kodzie na okres z danymi.

### Problem: Błąd kompilacji
**Rozwiązanie**: Uruchom `mvn clean compile` i sprawdź błędy

---

## 💡 Wskazówki

- **Tryb DEMO** jest najlepszy do szybkiego sprawdzenia czy wszystko działa
- **Tryb CLI** jest najlepszy do interaktywnego testowania funkcjonalności
- Jeśli chcesz zmienić daty w trybie DEMO, edytuj `SogaApplication.java`






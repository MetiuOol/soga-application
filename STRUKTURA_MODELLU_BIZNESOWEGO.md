# 📊 STRUKTURA MODELU BIZNESOWEGO

## 🎯 Przegląd

Stworzyliśmy uporządkowany model biznesowy, który wyraźnie dzieli odpowiedzialności i reprezentuje rzeczywistą strukturę biznesową.

---

## 📁 Struktura Pakietów

```
pl.kurs.sogaapplication
├── models
│   └── business/
│       ├── PointOfSale.java          # Model punktu sprzedaży
│       ├── WorkingHours.java         # Model godzin pracy
│       ├── SalesCategory.java        # Kategorie sprzedaży
│       └── CostAllocationStrategy.java  # Strategie alokacji kosztów
└── service
    └── config/
        ├── RestaurantConfigService.java  # Konfiguracja grup/kategorii
        └── PointOfSaleService.java       # Zarządzanie punktami sprzedaży
```

---

## 🔑 Kluczowe Komponenty

### 1. **PointOfSale** (Punkt Sprzedaży)

Model reprezentujący punkt sprzedaży w biznesie:
- **ID** - unikalny identyfikator (np. "KD", "RATUSZOWA")
- **Nazwa** - nazwa punktu (np. "Kuchnia Domowa", "Ratuszowa")
- **ID Użytkownika** - główny sprzedawca przypisany do punktu (opcjonalny)
- **Lista sprzedawców** - wszystkie ID sprzedawców przypisanych do punktu
- **Godziny pracy** - `WorkingHours` - kiedy punkt jest otwarty
- **Kategorie sprzedaży** - `SalesCategory` - jakie typy sprzedaży są dostępne

**Przykład:**
- **KD (Kuchnia Domowa)**: 
  - ID: "KD"
  - Sprzedawca: ID 11
  - Godziny: 11-18 pn-pt, 11-14 sobota
  - Kategorie: WEIGHT_BASED, SOUPS, SUBSCRIPTIONS, etc.

- **RATUSZOWA**:
  - ID: "RATUSZOWA"
  - Sprzedawcy: Wszyscy oprócz ID 11
  - Godziny: 12-21 codziennie
  - Kategorie: RESTAURANT_KITCHEN, RESTAURANT_BUFFET

---

### 2. **WorkingHours** (Godziny Pracy)

Model reprezentujący godziny pracy punktu:
- **Mapa dni tygodnia → zakresy czasowe** - `DayOfWeek` → `TimeRange`
- Metody:
  - `isOpen(DayOfWeek, LocalTime)` - sprawdza czy punkt jest otwarty
  - `getHoursForDay(DayOfWeek)` - zwraca zakres godzin dla danego dnia
  - `isOpenOnDay(DayOfWeek)` - sprawdza czy punkt pracuje danego dnia

**Wewnętrzna klasa `TimeRange`:**
- `openTime` - czas otwarcia
- `closeTime` - czas zamknięcia
- `contains(LocalTime)` - sprawdza czy czas jest w zakresie
- `getHours()` - liczy liczbę godzin pracy

---

### 3. **SalesCategory** (Kategorie Sprzedaży)

Enum reprezentujący kategorie sprzedaży:
- **RESTAURANT_KITCHEN** - Sprzedaż w restauracji (kuchnia)
- **RESTAURANT_BUFFET** - Sprzedaż w restauracji (bufet)
- **WEIGHT_BASED** - Sprzedaż na wagę
- **SOUPS** - Zupy
- **SUBSCRIPTIONS** - Abonamenty
- **REGULAR_VOUCHERS** - Karnety zwykłe
- **MEAT_VOUCHERS** - Karnety mięsne
- **TAKEAWAY_PACKAGING** - Opakowania na wynos

**Metody pomocnicze:**
- `isRestaurant()` - sprawdza czy kategoria należy do restauracji
- `isKuchniaDomowa()` - sprawdza czy kategoria należy do KD

---

### 4. **CostAllocationStrategy** (Strategie Alokacji Kosztów)

Interfejs reprezentujący strategię alokacji kosztów między punktami sprzedaży.

**Implementacje:**

1. **ProportionalToSalesStrategy** (Proporcjonalnie do sprzedaży)
   - Koszty dzielone w proporcji do przychodu każdego punktu
   - Najbardziej sprawiedliwe dla kosztów surowców

2. **ProportionalToHoursStrategy** (Proporcjonalnie do godzin)
   - Koszty dzielone w proporcji do liczby godzin pracy
   - Przydatne dla kosztów pracowników/czynszu

3. **HybridStrategy** (Strategia hybrydowa)
   - Część kosztów proporcjonalnie do sprzedaży
   - Część kosztów proporcjonalnie do godzin
   - Konfigurowalne wagi (np. 70% sprzedaż, 30% godziny)

**Metoda główna:**
```java
Map<String, BigDecimal> allocateCost(
    BigDecimal totalCost,
    Map<String, BigDecimal> salesByPoint,
    Map<String, Long> workingHoursByPoint
)
```

---

### 5. **PointOfSaleService** (Serwis Punktów Sprzedaży)

Serwis zarządzający punktami sprzedaży:
- Inicjalizuje punkty sprzedaży z konfiguracją
- Udostępnia punkty sprzedaży po ID
- Wyszukuje punkt sprzedaży po ID sprzedawcy
- Sprawdza przynależność sprzedawcy do punktu

**Metody:**
- `getPointOfSale(String id)` - zwraca punkt po ID
- `getPointOfSaleBySellerId(Integer sellerId)` - zwraca punkt dla sprzedawcy
- `getAllPointsOfSale()` - zwraca wszystkie punkty
- `isSellerInPointOfSale(Integer, String)` - sprawdza przynależność

---

## 🔄 Migracja z Koncept Enum

Stary enum `Koncept` został oznaczony jako `@Deprecated` ale pozostaje dla kompatybilności wstecznej.

**Zalecane użycie:**
```java
// Stare (deprecated):
Koncept koncept = Koncept.fromIdUzytkownika(11);

// Nowe (zalecane):
PointOfSale pos = pointOfSaleService.getPointOfSaleBySellerId(11)
    .orElseThrow();
```

---

## 💡 Korzyści Nowego Modelu

1. **Jasny podział odpowiedzialności** - każdy komponent ma jedno zadanie
2. **Rozszerzalność** - łatwe dodanie nowych punktów sprzedaży
3. **Konfigurowalność** - godziny pracy i kategorie łatwe do zmiany
4. **Alokacja kosztów** - gotowe strategie podziału kosztów
5. **Type safety** - użycie typów zamiast magic numbers
6. **Testowalność** - łatwe do testowania jednostkowego

---

## 📋 Następne Kroki

1. ✅ **Stworzenie modelu biznesowego** - GOTOWE
2. ⏳ **Integracja z istniejącymi serwisami** - TODO
3. ⏳ **Aktualizacja serwisów analizy** - TODO
4. ⏳ **Aktualizacja CLI** - TODO
5. ⏳ **Usunięcie starego Koncept enum** - PO MIGRACJI

---

## 🔧 Przykłady Użycia

### Pobranie punktu sprzedaży
```java
@Autowired
private PointOfSaleService pointOfSaleService;

// Pobierz punkt po ID
PointOfSale kd = pointOfSaleService.getPointOfSale("KD")
    .orElseThrow();

// Pobierz punkt dla sprzedawcy
PointOfSale pos = pointOfSaleService.getPointOfSaleBySellerId(11)
    .orElseThrow();
```

### Sprawdzenie godzin pracy
```java
DayOfWeek day = DayOfWeek.MONDAY;
LocalTime time = LocalTime.of(13, 0);

if (kd.getWorkingHours().isOpen(day, time)) {
    // Punkt jest otwarty
}
```

### Alokacja kosztów
```java
CostAllocationStrategy strategy = 
    new CostAllocationStrategy.ProportionalToSalesStrategy();

BigDecimal totalCost = BigDecimal.valueOf(1000);
Map<String, BigDecimal> sales = Map.of(
    "KD", BigDecimal.valueOf(600),
    "RATUSZOWA", BigDecimal.valueOf(400)
);
Map<String, Long> hours = Map.of(
    "KD", 40L,
    "RATUSZOWA", 63L
);

Map<String, BigDecimal> allocated = strategy.allocateCost(
    totalCost, sales, hours
);
// KD: 600 zł, RATUSZOWA: 400 zł
```

---

## 📝 Uwagi

- Model jest niezmienny (immutable) gdzie to możliwe
- Używa `Optional` dla bezpiecznego pobierania danych
- Zgodność wsteczna zachowana przez deprecated Koncept enum
- Gotowy do integracji z istniejącymi serwisami






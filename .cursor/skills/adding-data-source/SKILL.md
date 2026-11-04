---
name: adding-data-source
description: Dodawanie lub zmiana adaptera źródła danych o jakości powietrza (GIOŚ, Airly, PurpleAir, Sensor.community lub nowe API) w backendzie Spring Boot. Używaj, gdy użytkownik prosi o nowy adapter, podłączenie API, pobieranie danych z nowego źródła albo poprawki w istniejącym adapterze.
---

# Dodawanie źródła danych

Wzorzec to adapter Sensor.community: `backend/src/main/java/pl/shkumat/airquality/source/sensorcommunity/`. Nowy adapter ma wyglądać tak samo: prosto, jak w projekcie studenckim, bez dodatkowych warstw.

Przepływ: API → `<Nazwa>Adapter` (zwraca `List<SourceReading>`) → `MeasurementImportService.save()` → baza. `FetchScheduler` wywołuje adapter według harmonogramu. **Nie dodawaj logiki konkretnego źródła do `MeasurementImportService`.**

## Lista kroków

```
- [ ] 1. Sprawdź API skryptem z scripts/ i zapisz przykładową odpowiedź
- [ ] 2. Rekordy JSON i adapter w source/<nazwa>/
- [ ] 3. Metoda w FetchScheduler + ustawienia w application.properties
- [ ] 4. Klucz API (jeśli jest) w .env i .env.example
- [ ] 5. Test adaptera na zapisanej odpowiedzi
- [ ] 6. mvnw test + uruchomienie aplikacji i sprawdzenie bazy
- [ ] 7. Aktualizacja docs/specyfikacja.md (tabela mapowania źródeł)
```

### 1. Sprawdzenie API

Uruchom `scripts/check_<źródło>.py` (instrukcja w `README.md`). Surowa odpowiedź trafia do `scripts/data/`. Airly ma limit 100 zapytań dziennie, PurpleAir liczy punkty: nie odpytuj ich w pętli.

### 2. Adapter

- `<Nazwa>Record`: rekordy Javy tylko z potrzebnymi polami, `@JsonIgnoreProperties(ignoreUnknown = true)` na każdym, `@JsonProperty` dla nazw z podkreśleniem lub polskimi znakami.
- `<Nazwa>Adapter implements SourceAdapter`, `@Component`:
  - `fetchReadings()` tylko pobiera dane (`RestClient.create()`) i woła `toReadings(...)`.
  - `toReadings(...)` jest package-private i nie robi zapytań HTTP, żeby dało się go przetestować.
  - Parametry przez `@Value` w konstruktorze: `app.area.latitude`, `app.area.longitude`, `app.area.radius-km`, klucz np. `${AIRLY_API_KEY:}`.
- Reguły konwersji do `SourceReading`:
  - `externalId` jako `String`, czas zawsze `Instant` w UTC, `measuredAt` = koniec okresu uśredniania.
  - `averagingMinutes` według tabeli poniżej.
  - Pomiń odczyt, jeśli nie ma ani PM2,5, ani PM10 albo wartości nie da się sparsować. Jedno z nich może być `null`.
  - Ustaw `indoor`, jeśli API to podaje.

### 3. Harmonogram

W `FetchScheduler` dodaj pole z adapterem i metodę:

```java
@Scheduled(initialDelayString = "PT10S", fixedDelayString = "${app.<źródło>.fetch-interval}")
public void fetch<Nazwa>() {
    fetch(<nazwa>Adapter);
}
```

W `application.properties` dodaj `app.<źródło>.fetch-interval` (format ISO, np. `PT1H`).

### 4. Klucze API

Klucz tylko w `.env` (nie jest w repozytorium); w `.env.example` sama nazwa zmiennej bez wartości. Spring czyta `.env` przez `spring.config.import`. Nigdy nie wpisuj klucza w kod, testy ani `application.properties`.

### 5. Test

- `src/test/resources/<źródło>-sample.json`: skrócona prawdziwa odpowiedź z kroku 1 plus przypadki brzegowe (brak PM, śmieciowa wartość, czujnik wewnętrzny).
- `<Nazwa>AdapterTests` bez kontekstu Springa: `new <Nazwa>Adapter(...)`, JSON wczytany przez `JsonMapper.builder().build().readValue(...)`.
- Wzór: `SensorCommunityAdapterTests`.

### 6. Weryfikacja

1. `docker compose up -d` w katalogu głównym (testy `@SpringBootTest` potrzebują bazy).
2. `backend/mvnw.cmd test`, wszystkie testy muszą przejść.
3. `mvnw.cmd spring-boot:run`, poczekaj na log `<ŹRÓDŁO>: saved N readings`, zatrzymaj aplikację.
4. Sprawdź bazę: `docker compose exec -T db psql -U airquality -d airquality -c "select count(*) from sensor where source = '<ŹRÓDŁO>';"`.

### 7. Dokumentacja i commit

Uzupełnij tabelę „Mapowanie źródeł” w `docs/specyfikacja.md`. Komunikat commitu po angielsku, np. `Add GIOS adapter`. **Commit i push tylko po zgodzie użytkownika.**

## Specyfika źródeł

| Źródło | Uśrednianie | Na co uważać |
|---|---|---|
| Sensor.community | 5 min | gotowe; P1 = PM10, P2 = PM2,5, liczby jako stringi, czas UTC bez strefy |
| GIOŚ | 60 min | stacja = jeden czujnik; PM10 i PM2,5 to osobne stanowiska, wybieraj po dokładnym kodzie `PM10` / `PM2.5` i łącz po godzinie; stanowiska manualne zwracają HTTP 400 (pomiń); najnowsze godziny często mają `Wartość: null`; czas lokalny `Europe/Warsaw` → UTC; pola po polsku |
| Airly | 60 min | gotowe; nagłówek `apikey`; limit 100/dzień, jedno pobranie = 1 + liczba instalacji zapytań, harmonogram co 6 h i pomijanie po restarcie (`fetchedWithin`); używamy `history` (24 pełne godziny), nie `current` (średnia krocząca); nazwy `PM25`, `PM10` |
| PurpleAir | ok. 2 min | nagłówek `X-API-Key`; odpowiedź to `fields` + `data` (wiersze jako listy); pola `pm2.5_atm`, `pm10.0_atm`, `location_type` (0 = outdoor); czas Unix; we Wrocławiu 1 czujnik wewnętrzny |

## Pułapki

- Spring Boot 4 używa Jacksona 3: adnotacje z `com.fasterxml.jackson.annotation`, a `JsonMapper` z `tools.jackson.databind.json`.
- Encje: zasady Lomboka z `AGENTS.md` (bez `@Data`).
- Testy `@SpringBootTest` muszą mieć `properties = "app.fetch.enabled=false"`, żeby nie odpytywać prawdziwych API.

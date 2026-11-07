# Specyfikacja systemu

Stan na 30.10.2026, zgodny z prezentacją na seminarium dyplomowe (wersja v7, 17 slajdów). Zmiana po prezentacji (07.11.2026): PurpleAir zastąpiony przez LookO2, bo we Wrocławiu PurpleAir ma tylko 1 czujnik wewnętrzny.

## 1. Problem

- **Dane są rozproszone.** Każde źródło ma inne API, inny format danych, inną częstotliwość pomiarów i dokładność.
- **Oficjalna sieć jest rzadka.** We Wrocławiu jest 5 stacji państwowych (GIOŚ/WIOŚ), pomiar co godzinę.
- **Sieć obywatelska jest gęsta, ale zawodna.** Około 70 czujników Sensor.community we Wrocławiu (mapa maps.sensor.community, 28.10.2026: 76 widocznych), ale dane są zaszumione i zawierają błędy.
- **Lokalne zdarzenia giną w średniej miejskiej**, np. spalanie odpadów w jednym miejscu.

Problemy tanich czujników (głównie SDS011), które system musi uwzględnić:

- zabrudzenie kanału optycznego (kurz, pajęczyny, kondensat), awaria wentylatora;
- publikowanie surowych danych bez kalibracji; uszkodzony czujnik wysyła zera, a sieć ich nie filtruje;
- montaż w miejscach bez cyrkulacji powietrza (np. zaciszny kąt balkonu);
- skutek: zaniżone odczyty (0–10 µg/m³ PM2,5) mimo złej jakości powietrza.

## 2. Cel i zakres

**Cel główny:** aplikacja webowa agregująca dane o jakości powietrza z wielu źródeł, z jednym formatem danych, prezentacją na mapie i wykresach oraz modułem detekcji lokalnych anomalii PM2,5 i PM10.

**Zakres pracy:**

1. Agregacja danych z czterech źródeł.
2. Normalizacja i zapis: wspólny model danych, walidacja odczytów, szeregi czasowe.
3. Wizualizacja: mapa, wykresy historyczne, porównanie źródeł.
4. Detekcja anomalii: koncepcja, implementacja i testy.

Obszar testowy: Wrocław.

## 3. Źródła danych

| Źródło | Czujniki i pomiary | Gęstość | Dostęp do API |
|---|---|---|---|
| GIOŚ | stacje państwowe, dokładne, co godzinę | niska | otwarty |
| Airly | własne (kalibrowane) | wysoka | klucz API, dzienny limit zapytań |
| Sensor.community | obywatelskie (głównie SDS011), średnie z 5 minut | wysoka | otwarty, bez klucza |
| LookO2 | polska sieć tanich czujników, bieżący odczyt | średnia | token (beta, na prośbę mailową), nie częściej niż co 30 min |

Szczegóły techniczne zebrane podczas przygotowania prezentacji. **Przed implementacją sprawdzić w aktualnej dokumentacji:**

- **GIOŚ:** starsze usługi `/rest` są wycofywane, korzystamy z nowego API. Limit dla bieżących danych: 1500 żądań na minutę. Dokumentacja: powietrze.gios.gov.pl/pjp/content/api.
- **Sensor.community:** plik `data.json` zwraca średnie z ostatnich 5 minut. Dokumentacja: github.com/opendata-stuttgart/meta/wiki/APIs.
- **Airly:** klucz API, dzienny limit zapytań na darmowym planie. Dokumentacja: developer.airly.org. Według NCBR ponad 2800 czujników w Polsce.
- **LookO2:** API w wersji beta, bez gwarancji działania, tylko do użytku niekomercyjnego. Token na prośbę (kontakt@looko2.com), dane opóźnione o ok. 20 min względem serwisu. Dokumentacja: looko2.pl/aktualnosci/api.

Sprawdzone skryptami z `scripts/` (30.10.2026, Wrocław):

- **GIOŚ:** nowe API `api.gios.gov.pl/pjp-api/v1/rest`, nazwy pól po polsku (np. „Identyfikator stacji”, „Wartość”), czas lokalny. 5 stacji, ale bieżące dane PM ma tylko część stanowisk: PM10 na Korzeniowskiego i Orzechowej oraz PM2,5 na Na Grobli to stanowiska manualne (wyniki po 4–8 tygodniach), Bartnicza nie mierzy PM. Na Wiśniowej ostatnie godziny miały puste wartości. Wskaźnik trzeba wybierać po dokładnym kodzie `PM10` / `PM2.5`. Ponowne sprawdzenie 07.11.2026: bieżące dane ma tylko PM2,5 na Korzeniowskiego (20 ostatnich godzin); Wiśniowa nadal zwraca same `null`. W promieniu 10 km od centrum są tylko te 5 stacji, lista wszystkich 288 stacji mieści się na jednej stronie.
- **Sensor.community:** `data.sensor.community/airrohr/v1/filter/area=lat,lon,km`, w promieniu 10 km 45 czujników PM (głównie SDS011). P1 = PM10, P2 = PM2,5, czas w UTC. W odpowiedzi są też czujniki temperatury bez PM.
- **Airly:** w promieniu 10 km 16 instalacji. Limit darmowego klucza: 100 zapytań dziennie (nagłówek `X-RateLimit-Remaining-day`).
- **PurpleAir:** klucz działa, ale we Wrocławiu i okolicy jest tylko 1 czujnik i jest on wewnętrzny (indoor). Dlatego zrezygnowaliśmy z tego źródła.
- **LookO2** (07.11.2026): `api.looko2.com/?method=GetAll` zwraca ostatni odczyt wszystkich ok. 2200 czujników naraz (filtr obszaru robimy sami). Działa z tokenem przykładowym `XXX` z dokumentacji. W prostokącie wokół Wrocławia 48 czujników, z danymi z ostatniej doby 14 (2 z nich wysyłają same zera); reszta przestała działać w latach 2018–2026. Wartości to liczby całkowite w postaci stringów, czas jako Unix timestamp, okres uśredniania nie jest podany. Pole `Indoor` ma zawsze `"0"`, także dla czujników z nazwą sugerującą pomieszczenie.

Nowe źródło dodaje się przez adapter, bez zmian w reszcie systemu.

## 4. Wymagania funkcjonalne

**Dane**

- FR1: cykliczne pobieranie danych ze wszystkich źródeł.
- FR2: normalizacja formatów JSON do jednego modelu danych.
- FR3: zapis historii pomiarów w bazie szeregów czasowych.

**Prezentacja**

- FR4: mapa czujników kolorowana według jakości powietrza.
- FR5: wykresy historyczne, porównanie czujników i źródeł.
- FR6: filtrowanie po źródle i czasie.

**Analiza**

- FR7: detekcja anomalii PM2,5 i PM10.
- FR8: rozróżnienie anomalii lokalnej od wzrostu w całym mieście.
- FR9: oznaczanie wykrytych anomalii na mapie.

**Poza zakresem (pomysły na później, usunięte z prezentacji):** powiadomienia o anomaliach, eksport danych do CSV.

## 5. Wymagania niefunkcjonalne

- **Niezawodność i jakość danych:** ponowienia z opóźnieniem, obsługa limitów API i braków danych; walidacja i odrzucanie błędnych odczytów.
- **Wydajność i rozszerzalność:** szybka mapa i zapytania do bazy (cele liczbowe do ustalenia); nowe źródło danych przez adapter.
- **Testowanie:** testy jednostkowe i skrypty testowe w Pythonie.
- **Bezpieczeństwo i użyteczność:** brak kluczy w kodzie, logowanie zdarzeń; czytelny interfejs, działanie na telefonie.

## 6. Architektura

Przepływ danych od lewej do prawej:

```
Źródła danych        →  Pobieranie i normalizacja   →  Baza danych            →  Detekcja anomalii          →  API i frontend
GIOŚ API                asynchronicznie, ponowienia,    PostgreSQL + TimescaleDB   średnia ruchoma, Z-score,     Spring Boot REST,
Sensor.community        adaptery do wspólnego modelu    (szeregi czasowe)          porównanie z otoczeniem       React, Leaflet, Chart.js
Airly API
LookO2 API
```

Kluczowe decyzje projektowe:

- asynchroniczne pobieranie, odporne na awarie i limity API;
- jeden spójny model danych dla wszystkich źródeł;
- całość uruchamiana w kontenerach Docker;
- backend: Java 21, Spring Boot 4.1, Maven.

### Model danych

Schemat tworzą migracje Flyway (`backend/src/main/resources/db/migration`). Czas zawsze w UTC (`timestamptz`); `measured_at` to koniec okresu uśredniania.

- **`sensor`**: czujnik z dowolnego źródła. Pola: `source` (`GIOS`, `SENSOR_COMMUNITY`, `AIRLY`, `LOOKO2`), `external_id` (id w API źródła), `name`, `latitude`, `longitude`, `sensor_type`, `indoor`, `last_seen_at`. Para `source` + `external_id` jest unikalna.
- **`measurement`**: hypertable TimescaleDB po `measured_at`. Pola: `sensor_id`, `measured_at`, `pm25`, `pm10` (mogą być puste), `averaging_minutes`, `valid`, `invalid_reason`, `fetched_at`. Klucz główny (`sensor_id`, `measured_at`), więc ponowne pobranie tego samego pomiaru nie tworzy duplikatu.

Mapowanie źródeł:

| Źródło | Czujnik (`external_id`) | Uśrednianie | Uwagi |
|---|---|---|---|
| GIOŚ | id stacji | 60 min | PM10 i PM2,5 to osobne stanowiska, łączone w jeden wiersz po godzinie; czas lokalny (`Europe/Warsaw`) → UTC; pobieranie co 30 min, za każdym razem ok. 20 ostatnich godzin; stanowiska manualne (HTTP 400) i puste godziny pomijane |
| Sensor.community | `sensor.id` | 5 min | P1 = PM10, P2 = PM2,5 |
| Airly | id instalacji | 60 min | pole `history` (24 pełne godziny); pobieranie co 6 h, ok. 68 z 100 zapytań dziennie; po restarcie aplikacji pobieranie jest pomijane, jeśli dane są świeższe niż 6 h |
| LookO2 | `Device` (adres MAC) | nieznane, przyjęte 1 min | bieżące `PM25`, `PM10` (nie `AveragePM*`); pobieranie co 30 min, także po restarcie; czujniki w promieniu `app.area.radius-km` od centrum; pomijane odczyty starsze niż 1 h |

Błędne odczyty nie są usuwane, tylko oznaczane `valid = false`, żeby dało się policzyć, ile danych odrzuciła walidacja. Przyczyna trafia do `invalid_reason`:

- `OUT_OF_RANGE`: PM2,5 lub PM10 poniżej 0 albo powyżej 1000 µg/m³ (fizycznie niemożliwe w powietrzu zewnętrznym).
- `STUCK_VALUE`: czujnik podał te same PM2,5 i PM10 tyle razy z rzędu, ile wynosi `app.validation.stuck-count` (domyślnie 6, np. 30 min dla Sensor.community, 3 h dla LookO2). Łapie ciągi zer i zawieszone czujniki. LookO2 podaje liczby całkowite, więc przy czystym powietrzu powtórzenia są bardziej prawdopodobne; do sprawdzenia na zebranych danych.

Walidacja działa przy zapisie: odczyty z jednej partii są sortowane po czasie i porównywane z poprzednimi pomiarami czujnika z bazy. Celowo nie odrzucamy PM2,5 > PM10, bo tanie czujniki często tak raportują. Odczyty zaniżone względem sąsiadów (np. stale 1 µg/m³ przy tle 15) nie są tu łapane; to zadanie porównania z otoczeniem w module detekcji.

### REST API i frontend

- `GET /api/sensors`: wszystkie czujniki z ostatnim pomiarem (z ostatnich 24 h; starszy pomiar = brak aktualnych danych).
- `GET /api/sensors/{id}/measurements?hours=24`: historia pomiarów czujnika (maks. 7 dni).

Frontend (React + Vite, `frontend/`): mapa Wrocławia z czujnikami kolorowanymi według PM2,5 w skali indeksu GIOŚ (0–13, 13–35, 35–55, 55–75, 75–110, > 110 µg/m³), filtr źródeł, legenda, panel czujnika z wykresem PM2,5 i PM10 z ostatnich 24 h. Czujniki wewnętrzne nie są pokazywane. Dane odświeżane co 5 minut. API zwraca pole `valid`; czujnik z błędnym ostatnim odczytem jest szary na mapie, a błędne punkty na wykresie są pomijane (przerwa w linii).

## 7. Algorytm detekcji lokalnych anomalii

Idea: dla każdego czujnika sprawdzamy, czy pokazuje teraz coś nietypowego w porównaniu z tym, co zwykle pokazuje, i czy jego sąsiedzi pokazują to samo.

1. **Walidacja.** Odrzucenie odczytów na pewno błędnych (ciągi zer, wartości niemożliwe fizycznie); reguły w sekcji „Model danych”. Detekcja korzysta tylko z odczytów `valid = true`.
2. **Norma czujnika.** W oknie czasowym (np. kilka ostatnich godzin) liczymy średnią ruchomą μ i odchylenie standardowe σ. Każdy czujnik ma własną normę.
3. **Z-score.** Dla nowego pomiaru x: `z = (x − μ) / σ`. Przykład: μ = 20, σ = 3, x = 35 → z = 5.
4. **Porównanie z otoczeniem.** Wysoki Z-score + spokojni sąsiedzi i tło miejskie → **anomalia lokalna**. Wzrost wszędzie → ogólny wzrost w mieście (smog), nie anomalia lokalna.
5. **Wynik.** Anomalia lokalna jest oznaczana na mapie.

Dlaczego nie wystarczy próg bezwzględny (np. norma 50 µg/m³ dla PM10):

| Sytuacja | Próg 50 µg/m³ | Wysoki Z-score | Tło rośnie | Wynik |
|---|---|---|---|---|
| Czujnik 92, tło 25 | tak | tak | nie | anomalia lokalna |
| Lato: czujnik 8 → 35, tło 8 | nie | tak | nie | anomalia lokalna |
| Zimowy smog: wszystko 120 | tak | tak, na początku | tak | ogólny wzrost |
| Smog utrzymuje się kilka dni | tak | nie | — | brak anomalii, ale złe powietrze |

Próg mówi o zagrożeniu dla zdrowia, moduł detekcji o tym, gdzie i kiedy dzieje się coś nietypowego. Czujnik stale pokazujący zero nie zostanie wykryty żadną z tych metod; to zadanie walidacji.

Parametry do ustalenia eksperymentalnie: długość okna, próg Z-score (np. 3), sposób liczenia tła. Ocena: anomalie syntetyczne wstrzyknięte w dane oraz znane incydenty.

## 8. Plan realizacji

| Etap | Status (30.10.2026) |
|---|---|
| Analiza i projekt | w trakcie |
| Pobieranie danych i baza danych | w trakcie |
| Moduł detekcji anomalii | zaplanowane |
| Frontend (mapa, dashboard) | w trakcie (podstawowa mapa i wykresy działają) |
| Testy i walidacja | zaplanowane |
| Praca pisemna | zaplanowane |

Strategia: najpierw działający szkielet od źródeł do mapy, potem udoskonalanie algorytmu. Terminy do ustalenia z promotorem.

## 9. Ryzyka

| Ryzyko | Ograniczenie |
|---|---|
| Jakość danych z tanich czujników | walidacja odczytów, porównanie z sąsiadami i stacjami GIOŚ |
| Limity API | ponowienia z opóźnieniem, cache |
| Trudna weryfikacja detekcji (brak opisanych danych) | anomalie syntetyczne, znane incydenty |
| Napięty harmonogram | najpierw działający szkielet systemu |

## 10. Otwarte kwestie

- Jak liczyć tło miejskie: mediana czujników w promieniu kilku km czy najbliższa stacja GIOŚ (propozycja, nie decyzja).
- Długość okna, próg Z-score, metryki oceny detekcji (np. precision/recall na anomaliach syntetycznych).
- Cele liczbowe wydajności (czas ładowania mapy, czas zapytań).
- LookO2 zamiast PurpleAir: zmiana względem prezentacji, do potwierdzenia z promotorem. Czekamy na własny token; okres uśredniania odczytów nieznany (zapytać LookO2).
- Airly: 100 zapytań dziennie przy 16 instalacjach, jak często pobierać dane.
- GIOŚ: obecnie tylko 1 stanowisko PM z bieżącymi danymi (PM2,5 na Korzeniowskiego), więc „tło = najbliższa stacja GIOŚ” oznacza w praktyce jedną stację dla całego miasta.
- Zakres pracy do uzgodnienia z promotorem.

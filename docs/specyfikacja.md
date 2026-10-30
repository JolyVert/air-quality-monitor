# Specyfikacja systemu

Stan na 30.10.2026, zgodny z prezentacją na seminarium dyplomowe (wersja v7, 17 slajdów).

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
| PurpleAir | własne (sensory Plantower) | niska | klucz API (rozliczanie w punktach) |

Szczegóły techniczne zebrane podczas przygotowania prezentacji. **Przed implementacją sprawdzić w aktualnej dokumentacji:**

- **GIOŚ:** starsze usługi `/rest` są wycofywane, korzystamy z nowego API. Limit dla bieżących danych: 1500 żądań na minutę. Dokumentacja: powietrze.gios.gov.pl/pjp/content/api.
- **Sensor.community:** plik `data.json` zwraca średnie z ostatnich 5 minut. Dokumentacja: github.com/opendata-stuttgart/meta/wiki/APIs.
- **Airly:** klucz API, dzienny limit zapytań na darmowym planie. Dokumentacja: developer.airly.org. Według NCBR ponad 2800 czujników w Polsce.
- **PurpleAir:** klucz API, zapytania rozliczane w punktach. Dokumentacja: api.purpleair.com.

Sprawdzone skryptami z `scripts/` (30.10.2026, Wrocław):

- **GIOŚ:** nowe API `api.gios.gov.pl/pjp-api/v1/rest`, nazwy pól po polsku (np. „Identyfikator stacji”, „Wartość”), czas lokalny. 5 stacji, ale bieżące dane PM ma tylko część stanowisk: PM10 na Korzeniowskiego i Orzechowej oraz PM2,5 na Na Grobli to stanowiska manualne (wyniki po 4–8 tygodniach), Bartnicza nie mierzy PM. Na Wiśniowej ostatnie godziny miały puste wartości. Wskaźnik trzeba wybierać po dokładnym kodzie `PM10` / `PM2.5`.
- **Sensor.community:** `data.sensor.community/airrohr/v1/filter/area=lat,lon,km`, w promieniu 10 km 45 czujników PM (głównie SDS011). P1 = PM10, P2 = PM2,5, czas w UTC. W odpowiedzi są też czujniki temperatury bez PM.
- **Airly:** w promieniu 10 km 16 instalacji. Limit darmowego klucza: 100 zapytań dziennie (nagłówek `X-RateLimit-Remaining-day`).
- **PurpleAir:** klucz działa, ale we Wrocławiu i okolicy jest tylko 1 czujnik i jest on wewnętrzny (indoor).

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
PurpleAir API
```

Kluczowe decyzje projektowe:

- asynchroniczne pobieranie, odporne na awarie i limity API;
- jeden spójny model danych dla wszystkich źródeł;
- całość uruchamiana w kontenerach Docker.

## 7. Algorytm detekcji lokalnych anomalii

Idea: dla każdego czujnika sprawdzamy, czy pokazuje teraz coś nietypowego w porównaniu z tym, co zwykle pokazuje, i czy jego sąsiedzi pokazują to samo.

1. **Walidacja.** Odrzucenie odczytów na pewno błędnych (ciągi zer, wartości niemożliwe fizycznie).
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
| Frontend (mapa, dashboard) | zaplanowane |
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

- Maven czy Gradle; wersja Javy (zainstalowana 22, Spring Boot 3 wymaga ≥ 17).
- Jak liczyć tło miejskie: mediana czujników w promieniu kilku km czy najbliższa stacja GIOŚ (propozycja, nie decyzja).
- Długość okna, próg Z-score, metryki oceny detekcji (np. precision/recall na anomaliach syntetycznych).
- Cele liczbowe wydajności (czas ładowania mapy, czas zapytań).
- PurpleAir: we Wrocławiu praktycznie brak czujników (1, wewnętrzny). Zostawić jako czwarte źródło, rozszerzyć obszar czy zrezygnować?
- Airly: 100 zapytań dziennie przy 16 instalacjach, jak często pobierać dane.
- GIOŚ: tylko kilka stanowisk PM z bieżącymi danymi, co wpływa na pomysł „tło = najbliższa stacja GIOŚ”.
- Zakres pracy do uzgodnienia z promotorem.

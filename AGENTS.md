# AGENTS.md

Kontekst dla agentów AI pracujących w tym repozytorium.

## O projekcie

Praca inżynierska Antona Shkumata (promotor: dr Michał Piórek, Wydział Informatyki i Telekomunikacji, specjalność „Inteligentne systemy przemysłu 4.0”, semestr zimowy 2026/27).

Temat: **Rozproszony system monitorowania i detekcji lokalnych anomalii jakości powietrza na podstawie danych z sieci czujników z wykorzystaniem zewnętrznych interfejsów API.**

W skrócie: aplikacja webowa, która pobiera pomiary PM2,5 i PM10 z czterech źródeł (GIOŚ, Sensor.community, Airly, LookO2), sprowadza je do jednego modelu danych, pokazuje na mapie i wykresach oraz wykrywa lokalne anomalie (nagły wzrost na jednym czujniku przy spokojnym tle miejskim). Obszar testowy: Wrocław.

## Gdzie czego szukać

- `docs/specyfikacja.md`: źródła danych, wymagania FR1–FR9 i niefunkcjonalne, architektura, algorytm detekcji, plan prac, ryzyka, otwarte kwestie. **Główne źródło prawdy o tym, co budujemy.**
- `.cursor/skills/adding-data-source/SKILL.md`: jak dodać lub zmienić adapter źródła danych (kroki, testy, specyfika GIOŚ, Airly, LookO2).
- `docs/tlo-i-motywacja.md`: liczby o jakości powietrza (UE, Polska, Wrocław), problemy tanich czujników, bibliografia. Przydatne przy pisaniu pracy.
- Prezentacja na seminarium dyplomowe leży **poza repozytorium**: `C:\Users\Anton\Downloads\pres\prezentacja_wstepna_v7.pptx`, tekst wystąpienia: `C:\Users\Anton\Downloads\pres\tekst_wystapienia.md`. Specyfikacja w `docs/` jest z nią zgodna na dzień 30.10.2026.

## Najważniejsze decyzje (już podjęte)

- Cztery **równorzędne** źródła danych: GIOŚ, Sensor.community, Airly, LookO2. Każde podłączone przez osobny adapter do wspólnego modelu. PurpleAir był w prezentacji, ale odpadł 07.11.2026 (we Wrocławiu 1 czujnik wewnętrzny).
- Backend: Java 21 + Spring Boot 4.1 (Spring Web, Spring Data JPA), budowanie Maven (Maven Wrapper `mvnw`). Pobieranie danych asynchroniczne, z ponowieniami i obsługą limitów API.
- Lombok: w encjach JPA tylko `@Getter`, `@Setter` na polach, które mogą się zmieniać, i `@NoArgsConstructor(access = PROTECTED)`. **Bez `@Data`** i bez `@EqualsAndHashCode` na encjach.
- Baza: PostgreSQL + TimescaleDB (szeregi czasowe), schemat przez migracje Flyway.
- Frontend: React + Vite (JavaScript, bez TypeScriptu) + Leaflet.js (`react-leaflet`, mapa) + Chart.js (`react-chartjs-2`, wykresy). Interfejs po polsku. W trybie deweloperskim Vite przekierowuje `/api` na backend (port 8080), bez CORS.
- Uruchamianie: Docker (docker compose). Python do skryptów testowych i walidacji danych.
- Detekcja anomalii: Z-score PM2,5 w oknie vs własna historia, potem porównanie z medianą sąsiadów. Anomalia lokalna = wysoki Z-score **i** wartość wyraźnie wyższa od tła. Parametry w `application.properties` (`app.detection.*`), dobierane eksperymentalnie.

## Jeszcze nie ustalone

- Docelowe wartości okna, progu Z-score i mnożnika tła (są wartości startowe).
- Metryki oceny detekcji i detekcja PM10.
- Pełna lista w `docs/specyfikacja.md`, sekcja „Otwarte kwestie”.

## Środowisko (Windows 10)

- Zainstalowane: Git, Java 22 (projekt celuje w Javę 21, docelowo zainstalować JDK 21), Docker 29, Python 3.14 (wirtualne środowisko w `.venv`), Node.js 26 LTS. **Brak** GitHub CLI.
- Docker Desktop nie startuje sam: jeśli `docker` nie odpowiada, poproś użytkownika o uruchomienie. Testy backendu potrzebują bazy z `docker compose up -d`.
- Remote: `git@github.com:JolyVert/air-quality-monitor.git` (SSH działa).
- IDE użytkownika: IntelliJ IDEA (folder `E:\IdeaProjects`).

## Zasady

- Użytkownik pisze po rosyjsku albo po polsku. Odpowiadaj w języku jego wiadomości. Dokumentacja projektu w `docs/` i teksty do pracy są po polsku.
- Komunikaty commitów i `README.md` piszemy **po angielsku**.
- **Nie rób commitów ani pushy bez wyraźnej zgody użytkownika.** Przygotuj zmiany, pokaż je i zapytaj. Dotyczy też `git commit --amend`, rebase i force push.
- Klucze API (Airly, token LookO2) nigdy w kodzie ani w repozytorium. Trzymaj je w zmiennych środowiskowych lub w pliku `.env` (jest w `.gitignore`).
- Gdy zmieniasz zakres, źródła danych albo algorytm, zaktualizuj `docs/specyfikacja.md`, żeby dokumentacja zgadzała się z kodem i z prezentacją.

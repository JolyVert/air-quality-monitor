# Air Quality Monitor

A distributed system for monitoring and detecting local air quality anomalies based on sensor network data from external APIs.

Engineering thesis · Anton Shkumat · supervisor: dr Michał Piórek

## What the system does

- Periodically fetches PM2.5 and PM10 measurements from GIOŚ, Sensor.community, Airly and LookO2.
- Normalizes them into a single data model and stores them as time series.
- Shows sensors on a map and measurement history on charts.
- Detects local anomalies: a sudden rise at a single sensor that is not visible in the city background.

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4 (Spring Web, Spring Data JPA), Flyway, Maven |
| Database | PostgreSQL + TimescaleDB |
| Frontend | React, Vite, Leaflet.js, Chart.js |
| Tooling | Docker, Python (test scripts and data validation) |

## Project structure

| Path | Contents |
|---|---|
| `backend/` | Spring Boot application |
| `frontend/` | React application |
| `scripts/` | Python test and data validation scripts |
| `docs/` | Specification and background |
| `docker-compose.yml` | Local environment (PostgreSQL + TimescaleDB) |
| `.env.example` | Template for environment variables |

## Getting started

Run everything in Docker (requires only Docker):

```
cp .env.example .env    # fill in passwords and API keys
docker compose up -d --build
```

The map is on http://localhost:3000. Stop with `docker compose stop`; data is kept in the `db-data` volume.

For development (requires JDK 21+ and Node.js 20+), run only the database in Docker:

```
docker compose up -d db

cd backend
.\mvnw.cmd spring-boot:run    # backend on http://localhost:8080

cd frontend
npm install
npm run dev                   # map on http://localhost:5173
```

Backend tests need the database from `docker compose`: `cd backend && .\mvnw.cmd test`.

REST API:

- `GET /api/sensors`: all sensors with their latest measurement
- `GET /api/sensors/{id}/measurements?hours=24`: measurement history of one sensor

API check scripts (Python):

```
python -m venv .venv
.venv\Scripts\pip install -r scripts/requirements.txt
cd scripts
..\.venv\Scripts\python check_sensor_community.py
```

Other scripts: `check_gios.py`, `check_airly.py`, `check_looko2.py`. Raw responses are saved to `scripts/data/`.

## Documentation

- [Specification](docs/specyfikacja.md): requirements, architecture, algorithm, work plan.
- [Background and motivation](docs/tlo-i-motywacja.md): air quality data and bibliography.

## Status

Working: data fetching from all four sources, validation of readings, storage in TimescaleDB, map with sensors and 24-hour charts, local anomaly detection (PM2.5 Z-score vs neighbours).

Next: tune detection parameters on collected data, optionally add PM10 detection.

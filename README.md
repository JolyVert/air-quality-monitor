# Air Quality Monitor

A distributed system for monitoring and detecting local air quality anomalies based on sensor network data from external APIs.

Engineering thesis · Anton Shkumat · supervisor: dr Michał Piórek

## What the system does

- Periodically fetches PM2.5 and PM10 measurements from GIOŚ, Sensor.community, Airly and PurpleAir.
- Normalizes them into a single data model and stores them as time series.
- Shows sensors on a map and measurement history on charts.
- Detects local anomalies: a sudden rise at a single sensor that is not visible in the city background.

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java, Spring Boot (Spring Web, Spring Data JPA) |
| Database | PostgreSQL + TimescaleDB |
| Frontend | React, Leaflet.js, Chart.js |
| Tooling | Docker, Python (test scripts and data validation) |

## Documentation

- [Specification](docs/specyfikacja.md): requirements, architecture, algorithm, work plan.
- [Background and motivation](docs/tlo-i-motywacja.md): air quality data and bibliography.

## Status

Stage: analysis and design (October 2026). No code yet.

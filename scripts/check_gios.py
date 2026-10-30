"""Fetch current PM2.5 and PM10 from GIOS stations in Wroclaw."""

import requests

from common import save_json

BASE_URL = "https://api.gios.gov.pl/pjp-api/v1/rest"


def get_wroclaw_stations():
    response = requests.get(f"{BASE_URL}/station/findAll", params={"size": 500}, timeout=30)
    response.raise_for_status()
    stations = response.json()["Lista stacji pomiarowych"]
    return [s for s in stations if s["Nazwa miasta"] == "Wrocław"]


def get_pm_sensors(station_id):
    response = requests.get(f"{BASE_URL}/station/sensors/{station_id}", timeout=30)
    response.raise_for_status()
    sensors = response.json()["Lista stanowisk pomiarowych dla podanej stacji"]
    return [s for s in sensors if s["Wskaźnik - kod"] in ("PM10", "PM2.5")]


def get_measurements(sensor_id):
    response = requests.get(f"{BASE_URL}/data/getData/{sensor_id}", timeout=30)
    if response.status_code != 200:
        # manual sensors return 400, their results are published weeks later
        print(f"    no current data: {response.json().get('error_reason')}")
        return []
    return response.json()["Lista danych pomiarowych"]


def main():
    stations = get_wroclaw_stations()
    print(f"GIOS stations in Wroclaw: {len(stations)}")

    result = []
    for station in stations:
        print(f"\n{station['Nazwa stacji']} (id {station['Identyfikator stacji']})")
        station["sensors"] = []
        for sensor in get_pm_sensors(station["Identyfikator stacji"]):
            print(f"  {sensor['Wskaźnik - kod']} (sensor id {sensor['Identyfikator stanowiska']})")
            sensor["measurements"] = get_measurements(sensor["Identyfikator stanowiska"])
            # the newest hours are often still empty (None), print the first real value
            for m in sensor["measurements"]:
                if m["Wartość"] is not None:
                    print(f"    latest: {m['Wartość']} at {m['Data']}")
                    break
            else:
                print(f"    all {len(sensor['measurements'])} returned values are empty")
            station["sensors"].append(sensor)
        result.append(station)

    save_json("gios", result)


if __name__ == "__main__":
    main()

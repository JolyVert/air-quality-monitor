"""Fetch current PM2.5 and PM10 from PurpleAir sensors in Wroclaw.

Each request costs points (per sensor and per field), so only the needed fields are requested.
"""

import os

import requests

from common import WROCLAW_EAST, WROCLAW_NORTH, WROCLAW_SOUTH, WROCLAW_WEST, save_json

URL = "https://api.purpleair.com/v1/sensors"
FIELDS = "name,latitude,longitude,location_type,last_seen,pm2.5_atm,pm10.0_atm"


def main():
    api_key = os.getenv("PURPLEAIR_API_KEY")
    if not api_key:
        print("PURPLEAIR_API_KEY is not set in .env")
        return

    response = requests.get(URL, headers={"X-API-Key": api_key}, timeout=30, params={
        "fields": FIELDS,
        "nwlat": WROCLAW_NORTH,
        "nwlng": WROCLAW_WEST,
        "selat": WROCLAW_SOUTH,
        "selng": WROCLAW_EAST,
    })
    response.raise_for_status()
    data = response.json()

    # data comes as a list of rows, column names are in "fields"
    sensors = [dict(zip(data["fields"], row)) for row in data["data"]]
    print(f"PurpleAir sensors in Wroclaw: {len(sensors)}")
    for sensor in sensors:
        place = "outdoor" if sensor["location_type"] == 0 else "indoor"
        print(f"  {sensor['sensor_index']} ({sensor['name']}, {place}): "
              f"PM10={sensor['pm10.0_atm']} PM2.5={sensor['pm2.5_atm']} last_seen={sensor['last_seen']}")

    save_json("purpleair", data)


if __name__ == "__main__":
    main()

"""Fetch Airly installations in Wroclaw and current measurements for a few of them.

Every request counts towards the daily limit of the API key, so only a few
installations are checked.
"""

import os

import requests

from common import WROCLAW_LAT, WROCLAW_LON, WROCLAW_RADIUS_KM, save_json

BASE_URL = "https://airapi.airly.eu/v2"
MAX_MEASUREMENTS = 3


def main():
    api_key = os.getenv("AIRLY_API_KEY")
    if not api_key:
        print("AIRLY_API_KEY is not set in .env")
        return
    headers = {"apikey": api_key, "Accept": "application/json"}

    response = requests.get(f"{BASE_URL}/installations/nearest", headers=headers, timeout=30, params={
        "lat": WROCLAW_LAT,
        "lng": WROCLAW_LON,
        "maxDistanceKM": WROCLAW_RADIUS_KM,
        "maxResults": -1,
    })
    response.raise_for_status()
    installations = response.json()
    print(f"Airly installations in {WROCLAW_RADIUS_KM} km radius: {len(installations)}")

    for installation in installations[:MAX_MEASUREMENTS]:
        response = requests.get(f"{BASE_URL}/measurements/installation", headers=headers, timeout=30,
                                params={"installationId": installation["id"]})
        response.raise_for_status()
        current = response.json()["current"]
        installation["current"] = current

        values = {v["name"]: v["value"] for v in current["values"]}
        street = installation["address"].get("street")
        print(f"  {installation['id']} ({street}): PM10={values.get('PM10')} PM2.5={values.get('PM25')} "
              f"till {current['tillDateTime']}")

    print(f"Requests left today: {response.headers.get('X-RateLimit-Remaining-day')}")
    save_json("airly", installations)


if __name__ == "__main__":
    main()

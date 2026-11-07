"""Check how many LookO2 sensors in Wroclaw are still sending data.

GetAll returns every LookO2 sensor in one response. The API asks not to call it
more often than once per 30 minutes, otherwise the token can be blocked.
"""

import os
import time

import requests

from common import WROCLAW_EAST, WROCLAW_NORTH, WROCLAW_SOUTH, WROCLAW_WEST, save_json

URL = "https://api.looko2.com/"
# a sensor counts as active if its last measurement is newer than this
ACTIVE_HOURS = 24


def main():
    # "XXX" is the placeholder token from the LookO2 docs, a real one is given by kontakt@looko2.com
    token = os.getenv("LOOKO2_TOKEN", "XXX")
    response = requests.get(URL, params={"method": "GetAll", "token": token}, timeout=60)
    response.raise_for_status()
    data = response.json()
    print(f"LookO2 sensors in total: {len(data)}")

    now = time.time()
    active_total = sum(1 for s in data if now - float(s["Epoch"]) < ACTIVE_HOURS * 3600)
    print(f"Active in the last {ACTIVE_HOURS} h (whole network): {active_total}")

    in_wroclaw = []
    for sensor in data:
        lat = float(sensor["Lat"])
        lon = float(sensor["Lon"])
        if WROCLAW_SOUTH <= lat <= WROCLAW_NORTH and WROCLAW_WEST <= lon <= WROCLAW_EAST:
            in_wroclaw.append(sensor)

    print(f"Sensors in Wroclaw: {len(in_wroclaw)}")
    active = 0
    for sensor in in_wroclaw:
        hours_ago = (now - float(sensor["Epoch"])) / 3600
        if hours_ago < ACTIVE_HOURS:
            active += 1
        last = time.strftime("%Y-%m-%d %H:%M", time.gmtime(float(sensor["Epoch"])))
        print(f"  {sensor['Device']} ({sensor['Name']}): PM10={sensor['PM10']} PM2.5={sensor['PM25']} "
              f"indoor={sensor['Indoor']} last={last} UTC ({hours_ago:.0f} h ago)")
    print(f"Active in Wroclaw: {active}")

    save_json("looko2", data)


if __name__ == "__main__":
    main()

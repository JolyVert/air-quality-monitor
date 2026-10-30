"""Fetch the latest 5-minute averages from Sensor.community sensors around Wroclaw."""

import requests

from common import WROCLAW_LAT, WROCLAW_LON, WROCLAW_RADIUS_KM, save_json

URL = "https://data.sensor.community/airrohr/v1/filter/area={},{},{}"


def main():
    url = URL.format(WROCLAW_LAT, WROCLAW_LON, WROCLAW_RADIUS_KM)
    response = requests.get(url, timeout=60)
    response.raise_for_status()
    data = response.json()
    print(f"Records in {WROCLAW_RADIUS_KM} km radius: {len(data)}")

    # the area also contains temperature/humidity sensors, keep only PM sensors
    # P1 = PM10, P2 = PM2.5
    pm_records = []
    for record in data:
        values = {v["value_type"]: v["value"] for v in record["sensordatavalues"]}
        if "P1" in values and "P2" in values:
            pm_records.append(record)

    sensor_ids = {r["sensor"]["id"] for r in pm_records}
    print(f"PM records: {len(pm_records)}, unique PM sensors: {len(sensor_ids)}")

    types = {}
    for record in pm_records:
        name = record["sensor"]["sensor_type"]["name"]
        types[name] = types.get(name, 0) + 1
    print(f"Sensor types: {types}")

    for record in pm_records[:5]:
        values = {v["value_type"]: v["value"] for v in record["sensordatavalues"]}
        print(f"  sensor {record['sensor']['id']}: PM10={values['P1']} PM2.5={values['P2']} "
              f"at {record['timestamp']} UTC")

    save_json("sensor_community", data)


if __name__ == "__main__":
    main()

import json
import os
from datetime import datetime

from dotenv import load_dotenv

SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(SCRIPTS_DIR, "data")

load_dotenv(os.path.join(SCRIPTS_DIR, "..", ".env"))

# Wroclaw city center and a bounding box around the city
WROCLAW_LAT = 51.11
WROCLAW_LON = 17.03
WROCLAW_RADIUS_KM = 10

WROCLAW_NORTH = 51.21
WROCLAW_SOUTH = 51.04
WROCLAW_WEST = 16.80
WROCLAW_EAST = 17.18


def save_json(source, data):
    os.makedirs(DATA_DIR, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = os.path.join(DATA_DIR, f"{source}_{timestamp}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Saved raw data to {path}")

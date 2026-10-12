import csv
import json
import math
import random
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path
from weekly_energy_summary import calculate_weekly_summary


OUTPUT_DIR = Path("data")
JSON_OUTPUT = OUTPUT_DIR / "solar_power_7_days.json"
CSV_OUTPUT = OUTPUT_DIR / "solar_power_7_days.csv"

DEVICE_ID = "solar-array-01"
DAYS_TO_GENERATE = 7
INTERVAL_HOURS = 1

# Placeholder for demonstration only.
# I will replace this after the we all  agrees on the actual system capacity.
SIMULATED_PEAK_POWER_KW = 6.0


def generate_readings() -> list[dict]:
    readings = []
    today = date.today()
    first_day = today - timedelta(days=DAYS_TO_GENERATE - 1)

    for day_number in range(DAYS_TO_GENERATE):
        reading_day = first_day + timedelta(days=day_number)

        # clearer/cloudier days.
        daily_condition = random.uniform(0.55, 1.0)

        for hour in range(6, 19, INTERVAL_HOURS):
            #daylight curve: near zero at 06:00/18:00
            # and highest around midday.
            daylight_position = (hour - 6) / 12
            sunlight_curve = math.sin(
                math.pi * daylight_position
            )

            cloud_variation = random.uniform(0.88, 1.0)

            power_kw = (
                SIMULATED_PEAK_POWER_KW
                * daily_condition
                * sunlight_curve
                * cloud_variation
            )

            timestamp = datetime.combine(
                reading_day,
                time(hour=hour),
                tzinfo=timezone(timedelta(hours=2)),
            )

            readings.append({
                "deviceId": DEVICE_ID,
                "timestamp": timestamp.isoformat(),
                "powerKw": round(max(power_kw, 0.0), 2),
                "dataSource": "SIMULATED",
                "simulationVersion": "1.0",
            })

    return readings


def save_json(readings: list[dict]) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    with JSON_OUTPUT.open("w", encoding="utf-8") as file:
        json.dump(readings, file, indent=2)


def save_csv(readings: list[dict]) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    fieldnames = [
        "deviceId",
        "timestamp",
        "powerKw",
        "dataSource",
        "simulationVersion",
    ]

    with CSV_OUTPUT.open(
        "w",
        encoding="utf-8",
        newline="",
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=fieldnames,
        )
        writer.writeheader()
        writer.writerows(readings)


def main() -> None:
    readings = generate_readings()
    save_json(readings)
    save_csv(readings)

    summary = calculate_weekly_summary(readings)

    summary_output = (
        OUTPUT_DIR / "weekly_energy_summary.json"
    )

    with summary_output.open(
        "w",
        encoding="utf-8",
    ) as file:
        json.dump(summary, file, indent=2)

    print(f"Generated {len(readings)} simulated readings.")
    print(f"JSON: {JSON_OUTPUT}")
    print(f"CSV:  {CSV_OUTPUT}")
    print(f"Weekly summary saved to: {summary_output}")
    print()
    print("First 5 readings:")
    print(json.dumps(readings[:5], indent=2))
    print()
    print("Weekly energy summary:")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()

   # python C:\Users\libho\OneDrive\Desktop\farm-system\L1-sim\src\solar_power_generator.py
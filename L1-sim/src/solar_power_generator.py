import calendar
import csv
import json
import math
import random
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path

from weekly_energy_summary import calculate_weekly_summary
from monthly_energy_summary import calculate_monthly_summary


OUTPUT_DIR = Path("data")

JSON_OUTPUT = OUTPUT_DIR / "solar_power_12_months.json"
CSV_OUTPUT = OUTPUT_DIR / "solar_power_12_months.csv"
WEEKLY_SUMMARY_OUTPUT = OUTPUT_DIR / "weekly_energy_summary.json"
MONTHLY_SUMMARY_OUTPUT = OUTPUT_DIR / "monthly_energy_summary.json"

DEVICE_ID = "solar-array-01"
SIMULATED_PEAK_POWER_KW = 6.0

# Demonstration assumption only. Replace after agreeing
# on the farm system's capacity and location.
SIMULATION_VERSION = "1.0"


def simulate_day_condition() -> float:
    """Return one simulated clear/cloudy-day factor."""
    return random.uniform(0.55, 1.0)


def generate_readings_for_day(
    reading_day: date,
    daily_condition: float,
) -> list[dict]:
    """Generate hourly power samples for one simulated day."""
    readings = []

    # Each reading represents the following hour:
    # 06:00 represents 06:00-07:00; ... 17:00 represents 17:00-18:00.
    for hour in range(6, 18):
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
            "simulationVersion": SIMULATION_VERSION,
        })

    return readings


def generate_readings_for_last_12_months() -> list[dict]:
    """Generate hourly readings from the start of the month
    11 months ago through today.
    """
    today = date.today()

    # Start at the first day of the current month, then step
    # back 11 months to cover 12 calendar months.
    current_month_number = today.year * 12 + today.month - 1
    start_month_number = current_month_number - 11

    start_year = start_month_number // 12
    start_month = start_month_number % 12 + 1
    first_day = date(start_year, start_month, 1)

    readings = []
    reading_day = first_day

    while reading_day <= today:
        daily_condition = simulate_day_condition()
        readings.extend(
            generate_readings_for_day(
                reading_day,
                daily_condition,
            )
        )
        reading_day += timedelta(days=1)

    return readings


def save_json(
    records: list[dict],
    output_path: Path,
) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    with output_path.open("w", encoding="utf-8") as file:
        json.dump(records, file, indent=2)


def save_csv(
    records: list[dict],
    output_path: Path,
) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    if not records:
        return

    fieldnames = list(records[0].keys())

    with output_path.open(
        "w",
        encoding="utf-8",
        newline="",
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=fieldnames,
        )
        writer.writeheader()
        writer.writerows(records)


def main() -> None:
    readings = generate_readings_for_last_12_months()

    weekly_summary = calculate_weekly_summary(readings)
    monthly_summary = calculate_monthly_summary(readings)

    save_json(readings, JSON_OUTPUT)
    save_csv(readings, CSV_OUTPUT)
    save_json([weekly_summary], WEEKLY_SUMMARY_OUTPUT)
    save_json([monthly_summary], MONTHLY_SUMMARY_OUTPUT)

    print(f"Generated {len(readings)} simulated readings.")
    print(f"Readings JSON: {JSON_OUTPUT}")
    print(f"Readings CSV:  {CSV_OUTPUT}")
    print()
    print("Weekly energy summary:")
    print(json.dumps(weekly_summary, indent=2))
    print()
    print("Monthly energy summary:")
    print(json.dumps(monthly_summary, indent=2))


if __name__ == "__main__":
    main()
   # python C:\Users\libho\OneDrive\Desktop\farm-system\L1-sim\src\solar_power_generator.py
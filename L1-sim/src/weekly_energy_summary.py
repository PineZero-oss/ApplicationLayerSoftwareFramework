from collections import defaultdict
from typing import Any



def calculate_weekly_summary(
    readings: list[dict[str, Any]],
) -> dict[str, Any]:
    energy_by_date: dict[str, float] = defaultdict(float)

    for reading in readings:
        timestamp = reading["timestamp"]
        date = timestamp[:10]
        power_kw = float(reading["powerKw"])

        # The current simulator samples once per hour.
        # This treats the reading as representative of
        # the following one-hour interval.
        interval_hours = 1.0

        energy_by_date[date] += power_kw * interval_hours

    daily_totals = [
        {
            "date": date,
            "energyKwh": round(energy_kwh, 2),
        }
        for date, energy_kwh in sorted(energy_by_date.items())
    ]

    if not daily_totals:
        return {
            "deviceId": "solar-array-01",
            "periodStart": None,
            "periodEnd": None,
            "dataSource": "SIMULATED",
            "unit": "kWh",
            "totalEnergyKwh": 0.0,
            "averageDailyEnergyKwh": 0.0,
            "bestDay": None,
            "dailyTotals": [],
        }

    total_energy = sum(
        item["energyKwh"]
        for item in daily_totals
    )

    best_day = max(
        daily_totals,
        key=lambda item: item["energyKwh"],
    )

    return {
        "deviceId": readings[0]["deviceId"],
        "periodStart": daily_totals[0]["date"],
        "periodEnd": daily_totals[-1]["date"],
        "dataSource": "SIMULATED",
        "unit": "kWh",
        "totalEnergyKwh": round(total_energy, 2),
        "averageDailyEnergyKwh": round(
            total_energy / len(daily_totals),
            2,
        ),
        "bestDay": best_day,
        "dailyTotals": daily_totals,
    }
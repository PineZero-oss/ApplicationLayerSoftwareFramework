from collections import defaultdict
from typing import Any


def calculate_monthly_summary(
    readings: list[dict[str, Any]],
) -> dict[str, Any]:
    energy_by_month: dict[str, float] = defaultdict(float)

    for reading in readings:
        timestamp = reading["timestamp"]

        # The timestamp begins with YYYY-MM.
        month = timestamp[:7]

        power_kw = float(reading["powerKw"])

        # Assumption: one reading represents one hour.
        interval_hours = 1.0

        energy_by_month[month] += power_kw * interval_hours

    monthly_totals = [
        {
            "month": month,
            "energyKwh": round(energy_kwh, 2),
        }
        for month, energy_kwh in sorted(
            energy_by_month.items()
        )
    ]

    if not monthly_totals:
        return {
            "deviceId": "solar-array-01",
            "periodStart": None,
            "periodEnd": None,
            "dataSource": "SIMULATED",
            "unit": "kWh",
            "monthlyTotals": [],
        }

    return {
        "deviceId": readings[0]["deviceId"],
        "periodStart": monthly_totals[0]["month"],
        "periodEnd": monthly_totals[-1]["month"],
        "dataSource": "SIMULATED",
        "unit": "kWh",
        "monthlyTotals": monthly_totals,
    }
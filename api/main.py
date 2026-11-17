import json
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware


PROJECT_ROOT = Path(__file__).resolve().parent.parent

MESSAGES_FILE        = PROJECT_ROOT / "data" / "messages.jsonl"
MONTHLY_ENERGY_FILE  = PROJECT_ROOT / "data" / "monthly_energy_summary.json"
WEEKLY_ENERGY_FILE   = PROJECT_ROOT / "data" / "weekly_energy_summary.json"
SOLAR_12M_FILE       = PROJECT_ROOT / "data" / "solar_power_12_months.json"
SOLAR_7D_FILE        = PROJECT_ROOT / "data" / "solar_power_7_days.json"
SECURITY_EVENTS_FILE = PROJECT_ROOT / "L2-edge-gateway" / "data" / "security_events.json"

app = FastAPI(
    title="Farm IoT API",
    description="Dynamic access to the simulated farm data.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

def read_json(path: Path):
    if not path.exists():
        raise HTTPException(
            status_code=404,
            detail=f"Data file not found: {path.name}",
        )
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def read_jsonl(path: Path):
    if not path.exists():
        raise HTTPException(
            status_code=404,
            detail=f"Data file not found: {path.name}",
        )
    items = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                items.append(json.loads(line))
            except json.JSONDecodeError:
                continue   # skip corrupted line
    return items

@app.get("/")
def root():
    return {
        "status": "ok",
        "message": "Farm IoT API is running. Open /docs to explore.",
    }

@app.get("/api/messages")
def get_messages(
        device_id: Optional[str] = None,
        limit: int = Query(100, ge=1, le=10000),
        offset: int = Query(0, ge=0),
):
    items = read_jsonl(MESSAGES_FILE)
    if device_id:
        items = [m for m in items if m.get("deviceId") == device_id]
    total = len(items)
    return {
        "total": total,
        "count": len(items[offset:offset + limit]),
        "items": items[offset:offset + limit],
    }


@app.get("/api/messages/{device_id}")
def get_messages_for_device(device_id: str):
    items = read_jsonl(MESSAGES_FILE)
    filtered = [m for m in items if m.get("deviceId") == device_id]
    return {"deviceId": device_id, "count": len(filtered), "items": filtered}

@app.get("/api/energy/weekly")
def get_weekly_energy():
    return read_json(WEEKLY_ENERGY_FILE)


@app.get("/api/energy/weekly/days")
def get_weekly_days(
        start: Optional[str] = None,
        end: Optional[str] = None,
):
    data = read_json(WEEKLY_ENERGY_FILE)
    if not data:
        return []
    days = data[0].get("dailyTotals", [])
    if start:
        days = [d for d in days if d["date"] >= start]
    if end:
        days = [d for d in days if d["date"] <= end]
    return days

@app.get("/api/energy/monthly")
def get_monthly_energy():
    return read_json(MONTHLY_ENERGY_FILE)


@app.get("/api/energy/monthly/months")
def get_monthly_months():
    data = read_json(MONTHLY_ENERGY_FILE)
    if not data:
        return []
    return data[0].get("monthlyTotals", [])

@app.get("/api/solar/readings")
def get_solar_readings(
        start: Optional[str] = None,
        end: Optional[str] = None,
        limit: int = Query(100, ge=1, le=100000),
        offset: int = Query(0, ge=0),
):
    data = read_json(SOLAR_12M_FILE)
    if start:
        data = [r for r in data if r["timestamp"] >= start]
    if end:
        data = [r for r in data if r["timestamp"] <= end]
    total = len(data)
    return {
        "total": total,
        "count": len(data[offset:offset + limit]),
        "items": data[offset:offset + limit],
    }


@app.get("/api/solar/readings/7days")
def get_solar_readings_7days():
    return read_json(SOLAR_7D_FILE)

@app.get("/api/security/events")
def get_security_events(
        severity: Optional[str] = None,
        device_id: Optional[str] = None,
        event_type: Optional[str] = None,
        limit: int = Query(100, ge=1, le=10000),
        offset: int = Query(0, ge=0),
):
    data = read_json(SECURITY_EVENTS_FILE)
    if severity:
        data = [e for e in data if e.get("severity") == severity.upper()]
    if device_id:
        data = [e for e in data if e.get("deviceId") == device_id]
    if event_type:
        data = [e for e in data if e.get("eventType") == event_type]
    total = len(data)
    return {
        "total": total,
        "count": len(data[offset:offset + limit]),
        "items": data[offset:offset + limit],
    }


@app.get("/api/security/events/{event_id}")
def get_security_event(event_id: str):
    for event in read_json(SECURITY_EVENTS_FILE):
        if event.get("eventId") == event_id:
            return event
    raise HTTPException(status_code=404, detail="Event not found")

@app.get("/api/security/summary")
def security_summary():
    data = read_json(SECURITY_EVENTS_FILE)
    counts: dict[str, int] = {}
    for event in data:
        sev = event.get("severity", "UNKNOWN")
        counts[sev] = counts.get(sev, 0) + 1
    return {"totalEvents": len(data), "bySeverity": counts}
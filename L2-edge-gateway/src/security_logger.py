import json
from datetime import datetime,timedelta,timezone
from pathlib import Path
from uuid import uuid4


BASE_DIR = Path(__file__).resolve().parent.parent
SECURITY_LOG_FILE = BASE_DIR / "logs" / "security_events.jsonl"

SAST = timezone(timedelta(hours=2))

ALLOWED_SEVERITIES = {
    "LOW",
    "MEDIUM",
    "HIGH",
    "CRITICAL",
}

DEVICE_NAMES = {
    "incubator-01": "Incubator-Device 01",
    "solar-battery-01": "Solar-Battery 01",
    "chicken-house-01": "Chicken-House 01",
}


class SecurityEventLogger:
    def log_event(
            self, 
            event_type:str,
            severity:str,
            description:str,
            device_id: str | None = None,
            *,
            title: str,
            message: str,
            action: str = "MESSAGE_REJECTED",
            case_id: str | None = None,
    ) -> dict:
        severity = severity.upper()

        if severity not in ALLOWED_SEVERITIES:
            raise ValueError(
                f"Unsupported severity: {severity}. "
                "Use LOW, MEDIUM, HIGH, or CRITICAL."
            )

        device_display_name = DEVICE_NAMES.get(
            device_id,
            "Unknown device",
        )

        event = {
            "eventId": str(uuid4()),
            "eventType": event_type,
            "deviceId": device_id,
            "deviceDisplayName": device_display_name,
            "severity": severity,
            "title": title,
            "message": message,
            "occurredAt": datetime.now(SAST).isoformat(),
            "caseId": case_id,
            "dataSource": "SIMULATED",
            "technicalDescription": description,
            "action": action,
        }

        SECURITY_LOG_FILE.parent.mkdir(
            parents=True,
            exist_ok=True,
        )

        with SECURITY_LOG_FILE.open(
            "a",
            encoding="utf-8",
        ) as file:
            file.write(
                json.dumps(event, ensure_ascii=False) + "\n"
            )

        return event

    
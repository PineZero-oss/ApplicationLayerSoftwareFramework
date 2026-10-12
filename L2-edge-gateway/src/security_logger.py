import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

SECURITY_LOG_FILE = Path("logs/security_events.json1")

class SecurityEventLogger:
    def log_event(
            self, 
            event_type:str,
            severity:str,
            description:str,
            device_id: str | None = None,
            message: Any=None,
    ) -> dict[str, Any]:
        event ={
            "timestamp": datetime.now(
                timezone.utc
            ).isoformat(), 
            "eventType": event_type,
            "severity": severity,
            "deviceId": device_id,
            "description": description,
            "action": "MESSAGE_REJECTED",
            "message": message,
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
                json.dumps(event) + "\n"
            )
        return event
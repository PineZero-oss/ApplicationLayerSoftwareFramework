import json

from security_logger import (
    SECURITY_LOG_FILE,
    SecurityEventLogger,
)


def main() -> None:
    logger = SecurityEventLogger()

    event = logger.log_event(
        event_type="REPLAY_OR_DUPLICATE_DETECTED",
        severity="LOW",
        description=(
            "Sequence number 12 was received again "
            "from incubator-01."
        ),
        device_id="incubator-01",
        title="Repeated reading excluded",
        message=(
            "A repeated sensor update was excluded. "
            "This can happen when a device retries "
            "a transmission."
        ),
    )

    assert event["severity"] == "LOW"
    assert event["deviceDisplayName"] == "Incubator-Device 01"
    assert event["caseId"] is None
    assert SECURITY_LOG_FILE.exists()

    with SECURITY_LOG_FILE.open(
        encoding="utf-8",
    ) as file:
        lines = [
            line
            for line in file
            if line.strip()
        ]

    last_saved_event = json.loads(lines[-1])

    assert last_saved_event["eventId"] == event["eventId"]

    print("PASS: event created and saved.")
    print(f"Log location: {SECURITY_LOG_FILE}")
    print()
    print(json.dumps(event, indent=2))


if __name__ == "__main__":
    main()
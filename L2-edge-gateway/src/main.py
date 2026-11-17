from receiver import MessageReceiver


REGISTERED_DEVICES = {
    "incubator-01",
    "solar-battery-01",
    "chicken-house-01",
}


def validate_telemetry(message: object) -> tuple[bool, str]:
    required_fields = {
        "deviceId",
        "deviceType",
        "timestamp",
        "temperature",
        "humidity",
        "batteryPercentage",
        "eventType",
        "sequenceNumber",
    }

    if not isinstance(message, dict):
        return False, "Message is not a JSON object"

    missing_fields = required_fields - message.keys()

    if missing_fields:
        return (
            False,
            f"Missing fields: {sorted(missing_fields)}",
        )

    if message["deviceId"] not in REGISTERED_DEVICES:
        return False, "Unknown device"

    if not isinstance(message["temperature"], (int, float)):
        return False, "temperature must be numeric"

    if not isinstance(message["humidity"], (int, float)):
        return False, "humidity must be numeric"

    if not isinstance(
        message["batteryPercentage"],
        (int, float),
    ):
        return False, (
            "batteryPercentage must be numeric"
        )

    if not isinstance(message["sequenceNumber"], int):
        return False, (
            "sequenceNumber must be an integer"
        )

    if not 0 <= message["humidity"] <= 100:
        return False, (
            "humidity is outside valid range"
        )

    if not 0 <= message["batteryPercentage"] <= 100:
        return False, (
            "batteryPercentage is outside valid range"
        )

    if not -20 <= message["temperature"] <= 60:
        return False, (
            "temperature is outside valid range"
        )

    if message["eventType"] != "telemetry":
        return False, "Unexpected event type"

    if message["sequenceNumber"] < 1:
        return False, (
            "sequenceNumber must be positive"
        )

    return True, "Message accepted"


def display_result(
    title: str,
    result: str,
) -> None:
    print()
    print("=" * 60)
    print(title)
    print("=" * 60)
    print(result)


def main() -> None:
    receiver = MessageReceiver()

    valid_message = {
        "deviceId": "incubator-01",
        "deviceType": "incubator-sensor",
        "timestamp": "2026-09-29T10:00:00+00:00",
        "temperature": 37.5,
        "humidity": 61.2,
        "batteryPercentage": 78.0,
        "eventType": "telemetry",
        "sequenceNumber": 1,
    }

    invalid_json = (
        '{"deviceId": "incubator-01", '
        '"temperature": 37.5'
    )

    forged_message = {
        "deviceId": "incubator-01",
        "deviceType": "incubator-sensor",
        "timestamp": "2026-09-29T10:01:00+00:00",
        "temperature": 37.5,
        "humidity": 999.0,
        "batteryPercentage": 78.0,
        "eventType": "telemetry",
        "sequenceNumber": 2,
    }

    display_result(
        "VALID TELEMETRY TEST",
        receiver.receive_message(
            valid_message,
            validate_telemetry,
        ),
    )

    display_result(
        "INVALID JSON TEST",
        receiver.receive_message(
            invalid_json,
            validate_telemetry,
        ),
    )

    display_result(
        "FORGED RANGE TEST",
        receiver.receive_message(
            forged_message,
            validate_telemetry,
        ),
    )

    replay_test_message = {
        "deviceId": "incubator-01",
        "deviceType": "incubator-sensor",
        "timestamp": "2026-09-29T10:02:00+00:00",
        "temperature": 37.6,
        "humidity": 61.5,
        "batteryPercentage": 77.5,
        "eventType": "telemetry",
        "sequenceNumber": 3,
    }

    display_result(
        "REPLAY TEST: ORIGINAL MESSAGE",
        receiver.receive_message(
            replay_test_message,
            validate_telemetry,
        ),
    )

    display_result(
        "REPLAY TEST: REPEATED MESSAGE",
        receiver.receive_message(
            replay_test_message,
            validate_telemetry,
        ),
    )

    print()
    print("Layer 2 test completed.")
    print(
        "Check logs/security_events.jsonl "
        "for rejected-message events."
    )


if __name__ == "__main__":
    main()

    ## python C:\Users\Libho\OneDrive\Desktop\farm-system\L2-edge-gateway\src\main.py
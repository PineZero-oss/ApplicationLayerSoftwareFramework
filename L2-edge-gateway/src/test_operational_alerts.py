import time
from datetime import datetime, timezone

from receiver import MessageReceiver
from security_logger import SECURITY_LOG_FILE


def demo_validate(message: object) -> tuple[bool, str]:
    if not isinstance(message, dict):
        return False, "Message must be an object"

    required = {
        "deviceId",
        "sequenceNumber",
        "batteryPercentage",
        "temperature",
        "humidity",
    }

    if not required.issubset(message):
        return False, "Required fields are missing"

    if message["deviceId"] not in {
        "solar-battery-01",
        "incubator-01",
    }:
        return False, "Unknown device"

    for field in [
        "batteryPercentage",
        "temperature",
        "humidity",
    ]:
        if type(message[field]) not in (int, float):
            return False, f"{field} must be numeric"

    if type(message["sequenceNumber"]) is not int:
        return False, "Sequence must be an integer"

    if message["sequenceNumber"] < 1:
        return False, "Sequence must be positive"

    if not 0 <= message["batteryPercentage"] <= 100:
        return False, "Battery must be between 0 and 100"

    if not 0 <= message["humidity"] <= 100:
        return False, "Humidity must be between 0 and 100"

    if not -20 <= message["temperature"] <= 60:
        return False, "Temperature outside demo validation range"

    return True, "Valid demo reading"


def make_message(
    device_id: str,
    sequence: int,
    *,
    battery: float = 80,
    temperature: float = 37.5,
    humidity: float = 60,
) -> dict:
    return {
        "deviceId": device_id,
        "deviceType": "demo-sensor",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "eventType": "telemetry",
        "sequenceNumber": sequence,
        "batteryPercentage": battery,
        "temperature": temperature,
        "humidity": humidity,
    }


def main() -> None:
    # Demonstration settings only, not approved farm limits.
    receiver = MessageReceiver(
        device_timeout_seconds=2,
        incubator_temperature_range=(37.0, 38.0),
        incubator_humidity_range=(50.0, 70.0),
    )

    tests = [
        (
            "Normal battery: no alert expected",
            make_message(
                "solar-battery-01",
                1,
                battery=80,
            ),
        ),
        (
            "Low battery: HIGH event expected",
            make_message(
                "solar-battery-01",
                2,
                battery=25,
            ),
        ),
        (
            "Still low: no repeated battery event expected",
            make_message(
                "solar-battery-01",
                3,
                battery=24,
            ),
        ),
        (
            "Critical battery: CRITICAL event expected",
            make_message(
                "solar-battery-01",
                4,
                battery=10,
            ),
        ),
        
        (
            "Incubator temperature: HIGH event expected",
            make_message(
                "incubator-01",
                1,
                temperature=40,
            ),
        ),
        (
            "Incubator humidity: HIGH event expected",
            make_message(
                "incubator-01",
                2,
                temperature=37.5,
                humidity=80,
            ),
        ),
    ]

    for title, message in tests:
        print()
        print(title)

        result = receiver.receive_message(
            message,
            demo_validate,
        )

        print(result)

    print()
    print("Waiting for device-update timeout...")
    time.sleep(2.2)

    receiver.check_device_timeouts()
    print("Missing-update events checked.")

    print()
    print("Sending a fresh solar-battery reading...")

    restored_message = make_message(
        "solar-battery-01",
        5,
        battery=80,
    )

    print(
        receiver.receive_message(
            restored_message,
            demo_validate,
        )
    )

    print()
    print(f"Inspect the events here:\n{SECURITY_LOG_FILE}")


if __name__ == "__main__":
    main()
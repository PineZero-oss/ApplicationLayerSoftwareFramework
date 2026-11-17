import json
import logging
import random
import time
from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path

from validator import (validate_commmand, validate_telemetry,)


DATA_FILE = Path("data/messages.jsonl")
LOG_FILE = Path("data/simulator.log")


class IoTSimulator:
    def __init__(self) -> None:
        self.network_online = True
        self.offline_queue: list[dict] = []
        self.sequence_numbers = {
            "incubator-01": 0,
            "solar-battery-01": 0,
            "chicken-house-01": 0,
        }
        self.last_sequence_numbers: dict[str, int] = {}

    def generate_telemetry(
        self,
        device_id: str,
        device_type: str,
    ) -> dict:
        self.sequence_numbers[device_id] += 1

        sequence_number = self.sequence_numbers[device_id]

        return {
            "deviceId": device_id,
            "deviceType": device_type,
            "timestamp": datetime.now(
                timezone.utc
            ).isoformat(),
            "temperature": round(
                random.uniform(35.0, 40.0),
                2,
            ),
            "humidity": round(
                random.uniform(50.0, 75.0),
                2,
            ),
            "batteryPercentage": round(
                random.uniform(40.0, 100.0),
                2,
            ),
            "eventType": "telemetry",
            "sequenceNumber": sequence_number,
        }

    def save_message(self, message: object) -> None:
        DATA_FILE.parent.mkdir(
            parents=True,
            exist_ok=True,
        )

        with DATA_FILE.open(
            "a",
            encoding="utf-8",
        ) as file:
            if isinstance(message, str):
                file.write(message + "\n")
            else:
                file.write(
                    json.dumps(message) + "\n"
                )

    def transmit(self, message: dict) -> str:
        if not self.network_online:
            self.offline_queue.append(message)

            logging.warning(
                "Network offline. Queued message from %s",
                message["deviceId"],
            )

            return "queued_offline"

        logging.info(
            "Transmitted message from %s",
            message["deviceId"],
        )

        return "transmitted"

    def delay_message(
        self,
        message: dict,
        delay_seconds: float,
    ) -> str:
        print(
            f"Waiting {delay_seconds} seconds..."
        )

        time.sleep(delay_seconds)

        return self.transmit(message)

    def drop_message(self, message: dict) -> str:
        logging.warning(
            "Dropped message from %s",
            message["deviceId"],
        )

        return "dropped"

    def duplicate_message(
        self,
        message: dict,
    ) -> list[str]:
        first_result = self.transmit(message)

        duplicate = deepcopy(message)
        duplicate["duplicate"] = True

        second_result = self.transmit(duplicate)

        logging.warning(
            "Duplicated message from %s",
            message["deviceId"],
        )

        return [first_result, second_result]

    def disconnect_network(self) -> None:
        self.network_online = False

        logging.warning("Network disconnected")

    def reconnect_network(self) -> list[dict]:
        self.network_online = True

        queued_messages = self.offline_queue.copy()
        self.offline_queue.clear()

        logging.info(
            "Network reconnected. Releasing %d messages",
            len(queued_messages),
        )

        return queued_messages

    def malformed_messages(
        self,
        valid_message: dict,
    ) -> list[object]:
        incomplete_json = (
            '{"deviceId": "incubator-01", '
            '"temperature": 37.5'
        )

        wrong_data_types = deepcopy(valid_message)
        wrong_data_types["temperature"] = (
            "not-a-number"
        )
        wrong_data_types["sequenceNumber"] = "one"

        missing_field = deepcopy(valid_message)
        del missing_field["deviceId"]

        return [
            incomplete_json,
            wrong_data_types,
            missing_field,
        ]

    def forged_message(
        self,
        valid_message: dict,
    ) -> dict:
        forged = deepcopy(valid_message)

        forged["temperature"] = -50.0
        forged["humidity"] = 999.0
        forged["batteryPercentage"] = 500.0

        logging.warning(
            "Created forged or anomalous message",
        )

        return forged

    def replay_message(
        self,
        old_message: dict,
    ) -> dict:
        replayed = deepcopy(old_message)

        logging.warning(
            "Created replayed message with sequence number %s",
            replayed["sequenceNumber"],
        )

        return replayed

    def unauthorized_command(self) -> dict:
        command = {
            "deviceId": "unknown-device",
            "eventType": "command",
            "command": "shutdown-incubator",
            "apiKey": "invalid-api-key",
            "token": "expired-token",
        }

        logging.warning(
            "Created unauthorized command",
        )

        return command

    def receive_message(self, message: object) -> str:
        if isinstance(message, str):
            try:
                decoded_message = json.loads(message)
            except json.JSONDecodeError:
                logging.warning(
                    "Rejected message: invalid JSON"
                )
                return "REJECTED: invalid JSON"
            message = decoded_message 

        valid, reason = validate_telemetry(message)

        if not valid: 
            logging.warning(
                "Rejected message: %s",
                reason,
            )
            return f"REJECTED: {reason}"

        device_id = message["deviceId"]
        sequence_number = message["sequenceNumber"]
    
        last_sequence =self.last_sequence_numbers.get(
            device_id,
              0,
        )
    
        if sequence_number <= last_sequence:
            logging.warning(
                "Rejected replay from %s: "
                "sequence %s, last accepted %s",
                device_id,
                sequence_number,
                last_sequence,
            )
            return (
                "REJECTED: replayed or duplicated "
                "sequence number"
            )
        self.last_sequence_numbers[device_id] =(
            sequence_number
        )
    
        logging.info(
            "Accepted message from %s: sequence %s",
            device_id,
            sequence_number,
        )
    
        return "ACCEPTED: Message accepted"

      


    def receive_command(
            self, 
            command: dict, 
    ) -> str: 
        valid, reason = validate_commmand(command)

        if valid: 
            logging.info(
                "Accepted command from %s", 
                command["deviceId"],
            )
            return f"Accepted: {reason}"
        logging.warning(
            "Rejected commands: %s",
            reason,
        )
        return f"REJECTED: {reason}"


def configure_logging() -> None:
    LOG_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    logging.basicConfig(
        filename=LOG_FILE,
        level=logging.INFO,
        format=(
            "%(asctime)s | "
            "%(levelname)s | "
            "%(message)s"
        ),
    )


def display(title: str, value: object) -> None:
    print()
    print("=" * 60)
    print(title)
    print("=" * 60)

    if isinstance(value, str):
        print(value)
    else:
        print(
            json.dumps(
                value,
                indent=4,
            )
        )


def main() -> None:
    configure_logging()

    simulator = IoTSimulator()

    devices = [
        (
            "incubator-01",
            "incubator-sensor",
        ),
        (
            "solar-battery-01",
            "solar-battery-sensor",
        ),
        (
            "chicken-house-01",
            "environment-sensor",
        ),
    ]

    print("Starting farm IoT simulator")

    first_message = None
    print()
    print("VALIDATOR TEST")
    print("=" * 60)

    test_message= simulator.generate_telemetry(
         "incubator-01",
        "incubator-sensor",
    )
    print(
        simulator.receive_message(test_message)
    )

    for device_id, device_type in devices:
        message = simulator.generate_telemetry(
            device_id,
            device_type,
        )

        simulator.save_message(message)
        simulator.transmit(message)

        display(
            f"Normal telemetry: {device_id}",
            message,
        )

        if first_message is None:
            first_message = message

    delayed = simulator.generate_telemetry(
        "incubator-01",
        "incubator-sensor",
    )

    display(
        "Delayed message result",
        simulator.delay_message(delayed, 1),
    )

    dropped = simulator.generate_telemetry(
        "incubator-01",
        "incubator-sensor",
    )

    display(
        "Dropped message result",
        simulator.drop_message(dropped),
    )

    duplicate = simulator.generate_telemetry(
        "incubator-01",
        "incubator-sensor",
    )

    display(
        "Duplicate message results",
        simulator.duplicate_message(duplicate),
    )

    simulator.disconnect_network()

    offline_message = simulator.generate_telemetry(
        "solar-battery-01",
        "solar-battery-sensor",
    )

    display(
        "Offline message result",
        simulator.transmit(offline_message),
    )

    print(
        f"Messages currently queued: "
        f"{len(simulator.offline_queue)}"
    )

    queued_messages = simulator.reconnect_network()

    for message in queued_messages:
        print(
            "Reconnected transmission:",
            simulator.transmit(message),
        )

    malformed = simulator.malformed_messages(
        first_message
    )

    display(
        "Malformed message: incomplete JSON",
        malformed[0],
    )

    display(
        "Malformed message: wrong data types",
        malformed[1],
    )

    display(
        "Malformed message: missing field(the deviceId)",
        malformed[2],
    )

    print()
    print("VALIDATOR RESULTS FOR MALFORMED MESSAGES")
    print("=" * 60)

    for malformed_message in malformed:
        print(
            simulator.receive_message(
                malformed_message
            )
        )

    forged = simulator.forged_message(first_message)
    print()
    print("VALIDATOR RESULT FOR FORGED MESSAGE")
    print("=" *60)

    print(
        simulator.receive_message(forged)
    )

    display(
        "Forged message",
        forged,
    )


    replayed = simulator.replay_message(first_message)

    display(
        "Replayed old message",
        replayed,
    )

    original = simulator.generate_telemetry(
            "incubator-01",
            "incubator-sensor",
        )
    print(simulator.receive_message(original))
    
    
    replay = simulator.replay_message(original)
    print(simulator.receive_message(replay))

    command = simulator.unauthorized_command()
    print()
    print("VALIDATOR RESULT FOR UNAUTHORISED COMMAND")
    print("=" * 60)
    print(
        simulator.receive_command(command)
    )

    display(
        "Unauthorised command",
        command,
    )

    print()
    print("Simulation completed.")
    print(f"Messages saved to: {DATA_FILE}")
    print(f"Log saved to: {LOG_FILE}")


if __name__ == "__main__":
    main()

# run as  python C:\Users\libho\OneDrive\Desktop\farm-system\L1-sim\src\main.py
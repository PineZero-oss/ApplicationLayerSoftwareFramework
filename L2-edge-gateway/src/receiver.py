import json
from collections.abc import Callable
from operational_monitor import OperationalMonitor

from replay_protection import ReplayProtection
from security_logger import SecurityEventLogger


class MessageReceiver:
    def __init__(
        self,
        *,
        device_timeout_seconds: float = 300,
        incubator_temperature_range: tuple[float, float] | None = None,
        incubator_humidity_range: tuple[float, float] | None = None,
    ) -> None:
        self.replay_protection = ReplayProtection()
        self.security_logger = SecurityEventLogger()

        self.operational_monitor = OperationalMonitor(
            self.security_logger,
            device_timeout_seconds=device_timeout_seconds,
            incubator_temperature_range=(
                incubator_temperature_range
            ),
            incubator_humidity_range=(
                incubator_humidity_range
            ),
        )

    def check_device_timeouts(self) -> None:
      self.operational_monitor.check_missing_updates()

    def receive_message(
        self,
        message: object,
        validate_telemetry: Callable[
            [object],
            tuple[bool, str],
        ],
    ) -> str:
        if isinstance(message, str):
            try:
                message = json.loads(message)
            except json.JSONDecodeError:
                self.security_logger.log_event(
                    event_type="INVALID_JSON",
                    severity="MEDIUM",
                    description=(
                        "Incoming message could not be parsed as JSON."
                    ),
                    title="Sensor reading could not be processed",
                    message=(
                        "A sensor update could not be read and was "
                        "excluded from the monitoring data."
                    ),
                )

                return "REJECTED: invalid JSON"

        valid, reason = validate_telemetry(message)

        if not valid:
            device_id = self._get_device_id(message)

            self.security_logger.log_event(
                event_type="VALIDATION_FAILURE",
                severity="MEDIUM",
                description=reason,
                device_id=device_id,
                title="Sensor reading needs checking",
                message=(
                    "A sensor update contained missing or unexpected "
                    "information and was excluded from the monitoring "
                    "data."
                ),
            )

            return f"REJECTED: {reason}"

        device_id = message["deviceId"]
        sequence_number = message["sequenceNumber"]

        replay_valid, replay_reason = (
            self.replay_protection.check_message(
                device_id,
                sequence_number,
            )
        )

        if not replay_valid:
            self.security_logger.log_event(
                event_type="REPLAY_OR_DUPLICATE_DETECTED",
                severity="LOW",
                description=replay_reason,
                device_id=device_id,
                title="Repeated reading excluded",
                message=(
                    "A repeated or older sensor update was excluded "
                    "from the monitoring data."
                ),
            )

            return f"REJECTED: {replay_reason}"

        self.operational_monitor.observe_accepted_message(
             message
            )

        return "ACCEPTED: message accepted"

    @staticmethod
    def _get_device_id(message: object) -> str | None:
        if not isinstance(message, dict):
            return None

        device_id = message.get("deviceId")

        if isinstance(device_id, str):
            return device_id

        return None
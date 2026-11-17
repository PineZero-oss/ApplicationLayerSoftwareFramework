import math
import time

from security_logger import SecurityEventLogger


class OperationalMonitor:
    def __init__(
        self,
        logger: SecurityEventLogger,
        *,
        device_timeout_seconds: float = 300,
        battery_low_percent: float = 30,
        battery_critical_percent: float = 15,
        incubator_temperature_range: tuple[float, float] | None = None,
        incubator_humidity_range: tuple[float, float] | None = None,
    ) -> None:
        self.logger = logger
        self.device_timeout_seconds = device_timeout_seconds

        self.battery_low_percent = battery_low_percent
        self.battery_critical_percent = battery_critical_percent

        self.incubator_temperature_range = (
            incubator_temperature_range
        )
        self.incubator_humidity_range = (
            incubator_humidity_range
        )

        self.last_seen: dict[str, float] = {}
        self.device_missing: set[str] = set()
        self.condition_states: dict[tuple[str, str], str] = {}

    @staticmethod
    def _is_number(value: object) -> bool:
        return (
            isinstance(value, (int, float))
            and not isinstance(value, bool)
            and math.isfinite(value)
        )

    def _record_condition(
        self,
        *,
        device_id: str,
        condition: str,
        state: str,
        event_type: str,
        severity: str,
        title: str,
        message: str,
        description: str,
    ) -> None:
        key = (device_id, condition)
        previous_state = self.condition_states.get(
            key,
            "NORMAL",
        )

        if previous_state == state:
            return

        self.condition_states[key] = state

        if state == "NORMAL":
            return

        self.logger.log_event(
            event_type=event_type,
            severity=severity,
            description=description,
            device_id=device_id,
            title=title,
            message=message,
            action="ALERT_CREATED",
        )

    def observe_accepted_message(self, message: dict) -> None:
        device_id = message["deviceId"]

        self.last_seen[device_id] = time.monotonic()

        if device_id in self.device_missing:
            self.device_missing.remove(device_id)

            self.logger.log_event(
                event_type="DEVICE_COMMUNICATION_RESTORED",
                severity="LOW",
                description=(
                    "A new valid, non-replayed message arrived "
                    "after the device-update timeout."
                ),
                device_id=device_id,
                title="Device updates restored",
                message=(
                    "New readings are arriving from this device "
                    "again. Check the latest readings in the app."
                ),
                action="STATUS_UPDATED",
            )

        # Over here the solar-battery device drives this battery alert.
        if device_id == "solar-battery-01":
            self._check_battery(message)

        # and then over here the incubator drives these operating-range alerts.
        if device_id == "incubator-01":
            self._check_incubator(message)

    def _check_battery(self, message: dict) -> None:
        battery = message.get("batteryPercentage")

        if not self._is_number(battery):
            return

        if not 0 <= battery <= 100:
            return

        if battery <= self.battery_critical_percent:
            state = "CRITICAL"
            event_type = "SOLAR_BATTERY_CRITICAL"
            severity = "CRITICAL"
            title = "Solar battery critically low"
            user_message = (
                f"The solar battery is at {battery:.0f}%, below "
                "the configured critical level. The farm manager "
                "Check the power supply."
            )

        elif battery <= self.battery_low_percent:
            state = "LOW"
            event_type = "SOLAR_BATTERY_LOW"
            severity = "HIGH"
            title = "Solar battery running low"
            user_message = (
                f"The solar battery is at {battery:.0f}%, below "
                "the configured warning level."
            )

        else:
            state = "NORMAL"
            event_type = ""
            severity = "LOW"
            title = ""
            user_message = ""

        self._record_condition(
            device_id=message["deviceId"],
            condition="battery",
            state=state,
            event_type=event_type,
            severity=severity,
            title=title,
            message=user_message,
            description=(
                f"Battery={battery}%; "
                f"low threshold={self.battery_low_percent}%; "
                f"critical threshold="
                f"{self.battery_critical_percent}%."
            ),
        )

    def _check_incubator(self, message: dict) -> None:
        checks = [
            (
                "temperature",
                self.incubator_temperature_range,
                "INCUBATOR_TEMPERATURE_OUT_OF_RANGE",
                "Incubator temperature needs attention",
                "°C",
            ),
            (
                "humidity",
                self.incubator_humidity_range,
                "INCUBATOR_HUMIDITY_OUT_OF_RANGE",
                "Incubator humidity needs attention",
                "%",
            ),
        ]

        for field, limits, event_type, title, unit in checks:
            if limits is None:
                continue

            value = message.get(field)

            if not self._is_number(value):
                continue

            minimum, maximum = limits

            state = (
                "OUT_OF_RANGE"
                if value < minimum or value > maximum
                else "NORMAL"
            )

            self._record_condition(
                device_id=message["deviceId"],
                condition=field,
                state=state,
                event_type=event_type,
                severity="HIGH",
                title=title,
                message=(
                    f"The incubator reported {field} of "
                    f"{value:.1f}{unit}, outside its configured "
                    "operating range."
                ),
                description=(
                    f"{field}={value}; "
                    f"configured range={minimum} to {maximum}."
                ),
            )

    def check_missing_updates(self) -> None:
        now = time.monotonic()

        for device_id, last_seen in self.last_seen.items():
            elapsed = now - last_seen

            if (
                elapsed >= self.device_timeout_seconds
                and device_id not in self.device_missing
            ):
                self.device_missing.add(device_id)

                self.logger.log_event(
                    event_type="DEVICE_UPDATES_MISSING",
                    severity="HIGH",
                    description=(
                        f"No valid, non-replayed messages for "
                        f"{elapsed:.1f} seconds. Timeout is "
                        f"{self.device_timeout_seconds} seconds."
                    ),
                    device_id=device_id,
                    title="Device updates missing",
                    message=(
                        "New readings have stopped arriving from "
                        "this device. Displayed readings may be "
                        "out of date."
                    ),
                    action="ALERT_CREATED",
                )
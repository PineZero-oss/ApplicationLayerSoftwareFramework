import json
from typing import Any 

from replay_protection import ReplayProtection
from security_logger import SecurityEventLogger

class MessageReceiver:
    def __init__(self) -> None:
        self.replay_protection = ReplayProtection()
        self.security_logger = SecurityEventLogger() 

    def receive_message(
            self,
            message: object,
            validate_telmetry, 
    ) -> str:
        if isinstance(message, str):
            try:
                message = json.loads(message)
            except json.JSONDecodeError:
                self.security_logger.log_event(
                    event_type="INVALID_JSON",
                    severity="MEDIUM",
                    description="Incoming message is not valid JSON",
                    message= message, 
                )

                return "REJECTED: invalid JSON"
            valid, reason = validate_telmetry(message)

            if not valid:
                device_id = None

                if isinstance(message, dict):
                    device_id = message.get("deviceId")

                self.security_logger.log_event(
                    event_type="VALIDATION_FAILURE",
                    severity="MEDIUM",
                    description=reason,
                    device_id=device_id,
                    message=message, 
                )

                return f"REJECTED: {reason}"

            device_id = message["deviceId"]
            sequence_number = message["sequenceNumber"]

            replay_valid, replay_reason ={
                self.replay_protection.check_message(
                    device_id,
                    sequence_number,
                )
            }

            if not replay_valid:
                self.security_logger.log_event(
                    event_type="REPLAY_DETECTED",
                    severity="HIGH",
                    description= replay_reason,
                    device_id= device_id,
                    message= message,
                )

                return f"REJECTED: {replay_reason}"

            return "ACCEPTED: message accepted"

        
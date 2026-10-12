from typing import Any
from typing import Any 

FIELDS = {
    "deviceId",
    "deviceType",
    "timestamp",
    "humidity",
    "temperature",
    "batteryPercentage",
    "eventType",
    "sequenceNumber",
}

REGISTERED_DEVICES ={
    "incubator-01",
    "solar-battery-01",
    "chicken-house-01",
}

def validate_telemetry(
        message: Any,
) -> tuple[bool, str]:
    if not isinstance(message, dict):
        return False, "Message is not a JSON object"
    
    missing_fields = FIELDS - message.keys()

    if missing_fields:
        return(
            False, 
            f"Missing fields: {sorted(missing_fields)}",
        )

    if not isinstance(message["deviceId"], str):
        return False, "deviceId must be a string"
    if not isinstance(message["temperature"], (int, float)):
        return False, "temperature must be numeric"
    if not isinstance(message["humidity"], (int, float)):
        return False, "humidity must be numeric"
    if not isinstance(
        message["batteryPercentage"],
        (int,float),
    ): 
         return False, (
        "batteryPercentage must be numeric"
         )
    if not isinstance(message["sequenceNumber"], int):
        return False, (
            "sequenceNumber must be an integer"
        )
    if not 0 <=message["humidity"] <=100:
        return False, "humidity is outside valid range"
    if not 0 <= message["batteryPercentage"] <= 100:
        return False, (
            "batteryPercentage is outside valid range"
        )
    if not -20 <= message ["temperature"] <= 60:
        return False, (
            "temperature is outside valid range"
        )
    return True, "message accepted"

def validate_commmand(
        command: Any, 
) -> tuple[bool, str]:
    required_fields = {
        "deviceId",
        "eventType",
        "command",
        "apiKey",
        "token",
    }

    if not isinstance(command, dict): 
        return False, "Commmand is not a JSON object"
    missing_fields = required_fields -command.keys()

    if missing_fields:
        return(
            False, 
            f"Missing command fields: "
            f"{sorted(missing_fields)}",
        )
    if command["deviceId"] not in REGISTERED_DEVICES:
            return False, "Command device is not registered"
    if command["apiKey"] != "demo-valid-api-key":
        return False, "Invalid API key"
    if command["token"] == "expired-token":
        return False, "Token has expired"
    return True, "Command accepted"


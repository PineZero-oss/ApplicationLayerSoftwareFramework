import json
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent.parent

INPUT_FILE = BASE_DIR / "logs" / "security_events.jsonl"
OUTPUT_FILE = BASE_DIR / "data" / "security_events.json"


def main() -> None:
    print("Starting JSON export...")
    print(f"Reading events from: {INPUT_FILE}")

    if not INPUT_FILE.exists():
        print("No security log was found.")
        print("Run the operational-alert tests first.")
        return

    events = []

    with INPUT_FILE.open("r", encoding="utf-8") as file:
        for line_number, line in enumerate(file, start=1):
            if not line.strip():
                continue

            try:
                event = json.loads(line)
            except json.JSONDecodeError as error:
                print(
                    f"Export stopped: invalid JSON on "
                    f"line {line_number}: {error}"
                )
                return

            if not isinstance(event, dict):
                print(
                    f"Export stopped: line {line_number} "
                    "does not contain an event object."
                )
                return

            events.append(event)

    OUTPUT_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    with OUTPUT_FILE.open("w", encoding="utf-8") as file:
        json.dump(
            events,
            file,
            indent=2,
            ensure_ascii=False,
        )

    print(f"Exported {len(events)} events.")
    print(f"JSON file saved to: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
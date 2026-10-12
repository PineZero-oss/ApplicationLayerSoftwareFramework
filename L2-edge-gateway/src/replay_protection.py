class ReplayProtection: 
    def __init__(self) -> None: 
        self.last_sequence_numbers: dict[str, int] = {}

    def check_message(
            self, 
            device_id: str,
            sequence_number:int,
    ) -> tuple[bool, str]:
        last_sequence= self.last_sequence_numbers.get(
            device_id,
            0,
        )

        if sequence_number <= last_sequence:
            return(
                False, 
                "Replayed or duplicate sequence number"
            )
        self.last_sequence_numbers[device_id] = (
            sequence_number
        )

        return True, "Sequence number accepted"
    
        
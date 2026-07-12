from enum import Enum


class AppointmentStatus(str, Enum):
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"
    RESCHEDULED = "RESCHEDULED"
    SCHEDULED = "SCHEDULED"

    def __str__(self) -> str:
        return str(self.value)

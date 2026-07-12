from enum import Enum


class UserRole(str, Enum):
    ADMIN = "ADMIN"
    DOCTOR = "DOCTOR"
    PATIENT = "PATIENT"

    def __str__(self) -> str:
        return str(self.value)

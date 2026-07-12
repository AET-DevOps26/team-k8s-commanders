from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from attrs import field as _attrs_field

from ..models.appointment_status import AppointmentStatus
from ..types import UNSET, Unset

T = TypeVar("T", bound="Appointment")


@_attrs_define
class Appointment:
    """
    Attributes:
        id (UUID):
        patient_id (UUID):
        doctor_id (UUID):
        date_time (datetime.datetime):
        status (AppointmentStatus):
        duration (int): Duration in minutes
        reason (str | Unset):
    """

    id: UUID
    patient_id: UUID
    doctor_id: UUID
    date_time: datetime.datetime
    status: AppointmentStatus
    duration: int
    reason: str | Unset = UNSET
    additional_properties: dict[str, Any] = _attrs_field(init=False, factory=dict)

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        patient_id = str(self.patient_id)

        doctor_id = str(self.doctor_id)

        date_time = self.date_time.isoformat()

        status = self.status.value

        duration = self.duration

        reason = self.reason

        field_dict: dict[str, Any] = {}
        field_dict.update(self.additional_properties)
        field_dict.update(
            {
                "id": id,
                "patientId": patient_id,
                "doctorId": doctor_id,
                "dateTime": date_time,
                "status": status,
                "duration": duration,
            }
        )
        if reason is not UNSET:
            field_dict["reason"] = reason

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        patient_id = UUID(d.pop("patientId"))

        doctor_id = UUID(d.pop("doctorId"))

        date_time = datetime.datetime.fromisoformat(d.pop("dateTime"))

        status = AppointmentStatus(d.pop("status"))

        duration = d.pop("duration")

        reason = d.pop("reason", UNSET)

        appointment = cls(
            id=id,
            patient_id=patient_id,
            doctor_id=doctor_id,
            date_time=date_time,
            status=status,
            duration=duration,
            reason=reason,
        )

        appointment.additional_properties = d
        return appointment

    @property
    def additional_keys(self) -> list[str]:
        return list(self.additional_properties.keys())

    def __getitem__(self, key: str) -> Any:
        return self.additional_properties[key]

    def __setitem__(self, key: str, value: Any) -> None:
        self.additional_properties[key] = value

    def __delitem__(self, key: str) -> None:
        del self.additional_properties[key]

    def __contains__(self, key: str) -> bool:
        return key in self.additional_properties

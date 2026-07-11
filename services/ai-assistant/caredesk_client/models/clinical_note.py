from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from attrs import field as _attrs_field

from ..types import UNSET, Unset

if TYPE_CHECKING:
    from ..models.diagnosis import Diagnosis


T = TypeVar("T", bound="ClinicalNote")


@_attrs_define
class ClinicalNote:
    """
    Attributes:
        id (UUID):
        appointment_id (UUID):
        doctor_id (UUID):
        content (str):
        created_at (datetime.datetime):
        diagnosis (Diagnosis | Unset):
    """

    id: UUID
    appointment_id: UUID
    doctor_id: UUID
    content: str
    created_at: datetime.datetime
    diagnosis: Diagnosis | Unset = UNSET
    additional_properties: dict[str, Any] = _attrs_field(init=False, factory=dict)

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        appointment_id = str(self.appointment_id)

        doctor_id = str(self.doctor_id)

        content = self.content

        created_at = self.created_at.isoformat()

        diagnosis: dict[str, Any] | Unset = UNSET
        if not isinstance(self.diagnosis, Unset):
            diagnosis = self.diagnosis.to_dict()

        field_dict: dict[str, Any] = {}
        field_dict.update(self.additional_properties)
        field_dict.update(
            {
                "id": id,
                "appointmentId": appointment_id,
                "doctorId": doctor_id,
                "content": content,
                "createdAt": created_at,
            }
        )
        if diagnosis is not UNSET:
            field_dict["diagnosis"] = diagnosis

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.diagnosis import Diagnosis

        d = dict(src_dict)
        id = UUID(d.pop("id"))

        appointment_id = UUID(d.pop("appointmentId"))

        doctor_id = UUID(d.pop("doctorId"))

        content = d.pop("content")

        created_at = datetime.datetime.fromisoformat(d.pop("createdAt"))

        _diagnosis = d.pop("diagnosis", UNSET)
        diagnosis: Diagnosis | Unset
        if isinstance(_diagnosis, Unset):
            diagnosis = UNSET
        else:
            diagnosis = Diagnosis.from_dict(_diagnosis)

        clinical_note = cls(
            id=id,
            appointment_id=appointment_id,
            doctor_id=doctor_id,
            content=content,
            created_at=created_at,
            diagnosis=diagnosis,
        )

        clinical_note.additional_properties = d
        return clinical_note

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

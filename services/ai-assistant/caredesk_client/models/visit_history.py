from __future__ import annotations

from collections.abc import Mapping
from typing import TYPE_CHECKING, Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from attrs import field as _attrs_field

from ..types import UNSET, Unset

if TYPE_CHECKING:
    from ..models.appointment import Appointment
    from ..models.clinical_note import ClinicalNote


T = TypeVar("T", bound="VisitHistory")


@_attrs_define
class VisitHistory:
    """
    Attributes:
        patient_id (UUID):
        appointments (list[Appointment]):
        notes (list[ClinicalNote] | Unset):
    """

    patient_id: UUID
    appointments: list[Appointment]
    notes: list[ClinicalNote] | Unset = UNSET
    additional_properties: dict[str, Any] = _attrs_field(init=False, factory=dict)

    def to_dict(self) -> dict[str, Any]:
        patient_id = str(self.patient_id)

        appointments = []
        for appointments_item_data in self.appointments:
            appointments_item = appointments_item_data.to_dict()
            appointments.append(appointments_item)

        notes: list[dict[str, Any]] | Unset = UNSET
        if not isinstance(self.notes, Unset):
            notes = []
            for notes_item_data in self.notes:
                notes_item = notes_item_data.to_dict()
                notes.append(notes_item)

        field_dict: dict[str, Any] = {}
        field_dict.update(self.additional_properties)
        field_dict.update(
            {
                "patientId": patient_id,
                "appointments": appointments,
            }
        )
        if notes is not UNSET:
            field_dict["notes"] = notes

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        from ..models.appointment import Appointment
        from ..models.clinical_note import ClinicalNote

        d = dict(src_dict)
        patient_id = UUID(d.pop("patientId"))

        appointments = []
        _appointments = d.pop("appointments")
        for appointments_item_data in _appointments:
            appointments_item = Appointment.from_dict(appointments_item_data)

            appointments.append(appointments_item)

        _notes = d.pop("notes", UNSET)
        notes: list[ClinicalNote] | Unset = UNSET
        if _notes is not UNSET:
            notes = []
            for notes_item_data in _notes:
                notes_item = ClinicalNote.from_dict(notes_item_data)

                notes.append(notes_item)

        visit_history = cls(
            patient_id=patient_id,
            appointments=appointments,
            notes=notes,
        )

        visit_history.additional_properties = d
        return visit_history

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

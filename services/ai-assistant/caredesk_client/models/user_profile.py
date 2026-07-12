from __future__ import annotations

import datetime
from collections.abc import Mapping
from typing import Any, TypeVar
from uuid import UUID

from attrs import define as _attrs_define
from attrs import field as _attrs_field

from ..models.user_role import UserRole
from ..types import UNSET, Unset

T = TypeVar("T", bound="UserProfile")


@_attrs_define
class UserProfile:
    """
    Attributes:
        id (UUID):
        name (str):
        email (str):
        role (UserRole):
        clinic_id (UUID | Unset):
        phone_number (str | Unset):
        date_of_birth (datetime.date | Unset):
        specialization (str | Unset):
        license_number (str | Unset):
        enabled (bool | Unset): Whether the account is active. Disabled accounts cannot authenticate.
        password (str | Unset):
    """

    id: UUID
    name: str
    email: str
    role: UserRole
    clinic_id: UUID | Unset = UNSET
    phone_number: str | Unset = UNSET
    date_of_birth: datetime.date | Unset = UNSET
    specialization: str | Unset = UNSET
    license_number: str | Unset = UNSET
    enabled: bool | Unset = UNSET
    password: str | Unset = UNSET
    additional_properties: dict[str, Any] = _attrs_field(init=False, factory=dict)

    def to_dict(self) -> dict[str, Any]:
        id = str(self.id)

        name = self.name

        email = self.email

        role = self.role.value

        clinic_id: str | Unset = UNSET
        if not isinstance(self.clinic_id, Unset):
            clinic_id = str(self.clinic_id)

        phone_number = self.phone_number

        date_of_birth: str | Unset = UNSET
        if not isinstance(self.date_of_birth, Unset):
            date_of_birth = self.date_of_birth.isoformat()

        specialization = self.specialization

        license_number = self.license_number

        enabled = self.enabled

        password = self.password

        field_dict: dict[str, Any] = {}
        field_dict.update(self.additional_properties)
        field_dict.update(
            {
                "id": id,
                "name": name,
                "email": email,
                "role": role,
            }
        )
        if clinic_id is not UNSET:
            field_dict["clinicId"] = clinic_id
        if phone_number is not UNSET:
            field_dict["phoneNumber"] = phone_number
        if date_of_birth is not UNSET:
            field_dict["dateOfBirth"] = date_of_birth
        if specialization is not UNSET:
            field_dict["specialization"] = specialization
        if license_number is not UNSET:
            field_dict["licenseNumber"] = license_number
        if enabled is not UNSET:
            field_dict["enabled"] = enabled
        if password is not UNSET:
            field_dict["password"] = password

        return field_dict

    @classmethod
    def from_dict(cls: type[T], src_dict: Mapping[str, Any]) -> T:
        d = dict(src_dict)
        id = UUID(d.pop("id"))

        name = d.pop("name")

        email = d.pop("email")

        role = UserRole(d.pop("role"))

        _clinic_id = d.pop("clinicId", UNSET)
        clinic_id: UUID | Unset
        if isinstance(_clinic_id, Unset):
            clinic_id = UNSET
        else:
            clinic_id = UUID(_clinic_id)

        phone_number = d.pop("phoneNumber", UNSET)

        _date_of_birth = d.pop("dateOfBirth", UNSET)
        date_of_birth: datetime.date | Unset
        if isinstance(_date_of_birth, Unset):
            date_of_birth = UNSET
        else:
            date_of_birth = datetime.date.fromisoformat(_date_of_birth)

        specialization = d.pop("specialization", UNSET)

        license_number = d.pop("licenseNumber", UNSET)

        enabled = d.pop("enabled", UNSET)

        password = d.pop("password", UNSET)

        user_profile = cls(
            id=id,
            name=name,
            email=email,
            role=role,
            clinic_id=clinic_id,
            phone_number=phone_number,
            date_of_birth=date_of_birth,
            specialization=specialization,
            license_number=license_number,
            enabled=enabled,
            password=password,
        )

        user_profile.additional_properties = d
        return user_profile

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

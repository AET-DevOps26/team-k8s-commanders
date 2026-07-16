import uuid

import pytest
from fastapi import HTTPException

from models.user_role import UserRole
from utils.auth import require_roles, require_user_id


def test_require_user_id_returns_valid_gateway_uuid():
    user_id = uuid.uuid4()

    assert require_user_id(str(user_id)) == user_id


@pytest.mark.parametrize("header", [None, "", "   "])
def test_require_user_id_rejects_missing_identity(header):
    with pytest.raises(HTTPException) as error:
        require_user_id(header)

    assert error.value.status_code == 401
    assert error.value.detail == "Missing user id"


def test_require_user_id_rejects_malformed_identity():
    with pytest.raises(HTTPException) as error:
        require_user_id("not-a-uuid")

    assert error.value.status_code == 401
    assert error.value.detail == "Invalid user id"


def test_require_roles_accepts_allowed_gateway_role():
    dependency = require_roles(UserRole.DOCTOR, UserRole.ADMIN)

    assert dependency("DOCTOR") is UserRole.DOCTOR


@pytest.mark.parametrize(
    ("header", "status_code", "detail"),
    [
        (None, 401, "Missing user role"),
        ("", 401, "Missing user role"),
        ("OWNER", 403, "Unknown user role"),
    ],
)
def test_require_roles_rejects_missing_or_unknown_role(header, status_code, detail):
    dependency = require_roles(UserRole.DOCTOR)

    with pytest.raises(HTTPException) as error:
        dependency(header)

    assert error.value.status_code == status_code
    assert error.value.detail == detail


def test_require_roles_reports_allowed_roles_for_forbidden_caller():
    dependency = require_roles(UserRole.DOCTOR, UserRole.ADMIN)

    with pytest.raises(HTTPException) as error:
        dependency("PATIENT")

    assert error.value.status_code == 403
    assert error.value.detail == (
        "This endpoint requires one of the following roles: ADMIN, DOCTOR"
    )

"""Role-based authorization for AI Assistant endpoints.

The API gateway validates the JWT at the edge and forwards the caller's
identity to this service as the trusted ``X-User-Role`` header (see the
gateway's JwtAuthenticationFilter). This service does not re-validate the
JWT; it authorizes requests based on that header alone, so the header must
never be exposed to untrusted callers bypassing the gateway.
"""

import uuid
from collections.abc import Iterable

from fastapi import Header, HTTPException, status

from models.user_role import UserRole

# Headers set by the API gateway after JWT validation.
USER_ROLE_HEADER = "X-User-Role"
USER_ID_HEADER = "X-User-Id"


def require_user_id(
    x_user_id: str | None = Header(default=None, alias=USER_ID_HEADER),
) -> uuid.UUID:
    """FastAPI dependency resolving the authenticated caller's id.

    Sessions are owned per-user, so endpoints need the gateway-supplied
    ``X-User-Id``. A missing or non-UUID value means the request did not arrive
    with a valid identity from the gateway and is rejected as unauthenticated.
    """
    if x_user_id is None or not x_user_id.strip():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing user id",
        )
    try:
        return uuid.UUID(x_user_id)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid user id",
        )


def require_roles(*allowed_roles: UserRole):
    """Build a FastAPI dependency enforcing one of ``allowed_roles``.

    Returns 401 when the role header is missing (request did not pass through
    the gateway or carried no identity) and 403 when the role is present but
    not permitted for the endpoint.
    """
    allowed: frozenset[UserRole] = frozenset(allowed_roles)

    def _dependency(
        x_user_role: str | None = Header(default=None, alias=USER_ROLE_HEADER),
    ) -> UserRole:
        if x_user_role is None or not x_user_role.strip():
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Missing user role",
            )

        try:
            role = UserRole(x_user_role)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Unknown user role",
            )

        if role not in allowed:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=_forbidden_detail(allowed),
            )
        return role

    return _dependency


def _forbidden_detail(allowed: Iterable[UserRole]) -> str:
    roles = ", ".join(sorted(r.value for r in allowed))
    return f"This endpoint requires one of the following roles: {roles}"

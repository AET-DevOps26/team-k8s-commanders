"""Role-based authorization for AI Assistant endpoints.

The API gateway validates the JWT at the edge and forwards the caller's
identity to this service as the trusted ``X-User-Role`` header (see the
gateway's JwtAuthenticationFilter). This service does not re-validate the
JWT; it authorizes requests based on that header alone, so the header must
never be exposed to untrusted callers bypassing the gateway.
"""

from collections.abc import Iterable

from fastapi import Header, HTTPException, status

from models.user_role import UserRole

# Header set by the API gateway after JWT validation.
USER_ROLE_HEADER = "X-User-Role"


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

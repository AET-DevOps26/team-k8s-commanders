from http import HTTPStatus
from typing import Any
from urllib.parse import quote
from uuid import UUID

import httpx

from ... import errors
from ...client import AuthenticatedClient, Client
from ...models.appointment import Appointment
from ...models.problem_detail import ProblemDetail
from ...types import Response


def _get_kwargs(
    appointment_id: UUID,
) -> dict[str, Any]:

    _kwargs: dict[str, Any] = {
        "method": "get",
        "url": "/appointments/{appointment_id}".format(
            appointment_id=quote(str(appointment_id), safe=""),
        ),
    }

    return _kwargs


def _parse_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Appointment | ProblemDetail | None:
    if response.status_code == 200:
        response_200 = Appointment.from_dict(response.json())

        return response_200

    if response.status_code == 400:
        response_400 = ProblemDetail.from_dict(response.json())

        return response_400

    if response.status_code == 401:
        response_401 = ProblemDetail.from_dict(response.json())

        return response_401

    if response.status_code == 403:
        response_403 = ProblemDetail.from_dict(response.json())

        return response_403

    if response.status_code == 404:
        response_404 = ProblemDetail.from_dict(response.json())

        return response_404

    if response.status_code == 500:
        response_500 = ProblemDetail.from_dict(response.json())

        return response_500

    if client.raise_on_unexpected_status:
        raise errors.UnexpectedStatus(response.status_code, response.content)
    else:
        return None


def _build_response(
    *, client: AuthenticatedClient | Client, response: httpx.Response
) -> Response[Appointment | ProblemDetail]:
    return Response(
        status_code=HTTPStatus(response.status_code),
        content=response.content,
        headers=response.headers,
        parsed=_parse_response(client=client, response=response),
    )


def sync_detailed(
    appointment_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Appointment | ProblemDetail]:
    """Get appointment details

    Args:
        appointment_id (UUID):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Appointment | ProblemDetail]
    """

    kwargs = _get_kwargs(
        appointment_id=appointment_id,
    )

    response = client.get_httpx_client().request(
        **kwargs,
    )

    return _build_response(client=client, response=response)


def sync(
    appointment_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Appointment | ProblemDetail | None:
    """Get appointment details

    Args:
        appointment_id (UUID):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Appointment | ProblemDetail
    """

    return sync_detailed(
        appointment_id=appointment_id,
        client=client,
    ).parsed


async def asyncio_detailed(
    appointment_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Response[Appointment | ProblemDetail]:
    """Get appointment details

    Args:
        appointment_id (UUID):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Response[Appointment | ProblemDetail]
    """

    kwargs = _get_kwargs(
        appointment_id=appointment_id,
    )

    response = await client.get_async_httpx_client().request(**kwargs)

    return _build_response(client=client, response=response)


async def asyncio(
    appointment_id: UUID,
    *,
    client: AuthenticatedClient | Client,
) -> Appointment | ProblemDetail | None:
    """Get appointment details

    Args:
        appointment_id (UUID):

    Raises:
        errors.UnexpectedStatus: If the server returns an undocumented status code and Client.raise_on_unexpected_status is True.
        httpx.TimeoutException: If the request takes longer than Client.timeout.

    Returns:
        Appointment | ProblemDetail
    """

    return (
        await asyncio_detailed(
            appointment_id=appointment_id,
            client=client,
        )
    ).parsed

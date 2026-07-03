from .arbeitnow_connector import ArbeitnowConnector
from .base import JobConnector, NormalizedJob
from .greenhouse_connector import GreenhouseConnector
from .lever_connector import LeverConnector
from .mock_connector import MockConnector
from .remoteok_connector import RemoteOkConnector

ALL_CONNECTORS: list[JobConnector] = [
    RemoteOkConnector(),
    ArbeitnowConnector(),
    GreenhouseConnector(),
    LeverConnector(),
    MockConnector(),
]

__all__ = [
    "JobConnector",
    "NormalizedJob",
    "ALL_CONNECTORS",
    "RemoteOkConnector",
    "ArbeitnowConnector",
    "GreenhouseConnector",
    "LeverConnector",
    "MockConnector",
]

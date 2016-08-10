from enum import Enum
from typing import List, Any

import yass
from tutorial.base_types_external import Integer
from ... import contract


class Bond(contract.Instrument):
    def __init__(self) -> None:
        super().__init__()
        self.coupon = None  # type: float
        self.expiration = None  # type: contract.Expiration


class InstrumentService:
    MAPPER = yass.MethodMapper([
        yass.MethodMapping(0, 'getInstruments', False),
        yass.MethodMapping(1, 'showOneWay', True),
    ])

    def getInstruments(self) -> List[contract.Instrument]:
        raise NotImplementedError()

    def showOneWay(self, testBoolean: bool, testInt: Integer) -> None:
        raise NotImplementedError()

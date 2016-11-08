from enum import Enum
from typing import List, Any, cast

import yass
from tutorial.base_types_external import Integer
from ... import contract


class Bond(contract.Instrument):
    def __init__(self) -> None:
        contract.Instrument.__init__(self)
        self.coupon = cast('float', None)  # type: float
        self.expiration = cast('contract.Expiration', None)  # type: contract.Expiration


class InstrumentService:
    def getInstruments(self) -> List[contract.Instrument]:
        raise NotImplementedError()

    def showOneWay(self, testBoolean: bool, testInt: Integer) -> None:
        raise NotImplementedError()

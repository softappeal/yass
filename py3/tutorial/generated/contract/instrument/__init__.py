from enum import Enum
from typing import List, Any, cast

import yass
from tutorial.base_types_external import Integer
from ... import contract


class Bond(contract.Instrument):
    def __init__(self) -> None:
        contract.Instrument.__init__(self)
        self.coupon: float = cast(float, None)
        self.expiration: contract.Expiration = cast(contract.Expiration, None)


class InstrumentService:
    def getInstruments(self) -> List[contract.Instrument]:
        raise NotImplementedError()

    def showOneWay(self, testBoolean: bool, testInt: Integer) -> None:
        raise NotImplementedError()

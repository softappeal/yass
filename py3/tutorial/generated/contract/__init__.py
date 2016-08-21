from enum import Enum
from typing import List, Any

import yass
from tutorial.base_types_external import Integer


# shows how to use contract internal base types

class ExpirationHandler(yass.BaseTypeHandler):
    def readBase(self, reader: yass.Reader) -> 'Expiration':
        return Expiration(
            reader.readZigZagInt(),
            reader.readZigZagInt(),
            reader.readZigZagInt()
        )

    def writeBase(self, value: 'Expiration', writer: yass.Writer) -> None:
        writer.writeZigZagInt(value.year)
        writer.writeZigZagInt(value.month)
        writer.writeZigZagInt(value.day)


class Expiration:
    TYPE_DESC = yass.TypeDesc(yass.FIRST_DESC_ID + 1, ExpirationHandler())

    def __init__(self, year: int, month: int, day: int) -> None:
        self.year = year
        self.month = month
        self.day = day

    def __str__(self) -> str:
        return '%s-%s-%s' % (self.year, self.month, self.day)


class PriceKind(Enum):
    BID = 0
    ASK = 1


class Price:
    def __init__(self) -> None:
        self.instrumentId = None  # type: Integer
        self.kind = None  # type: PriceKind
        self.value = None  # type: Integer


@yass.abstract
class Instrument:
    def __init__(self) -> None:
        self.id = None  # type: Integer
        self.name = None  # type: str


class SystemException(Exception):
    def __init__(self) -> None:
        self.message = None  # type: str


@yass.abstract
class ApplicationException(Exception):
    def __init__(self) -> None:
        pass


class UnknownInstrumentsException(ApplicationException):
    def __init__(self) -> None:
        ApplicationException.__init__(self)
        self.instrumentIds = None  # type: List[Integer]
        self.onlyNeededForTests1 = None  # type: Any
        self.onlyNeededForTests2 = None  # type: bytes
        self.onlyNeededForTests3 = None  # type: Exception


class Node:
    def __init__(self) -> None:
        self.id = None  # type: float
        self.links = None  # type: List[Node]


class EchoService:
    def echo(self, value: Any) -> Any:
        raise NotImplementedError()


class PriceEngine:
    def subscribe(self, instrumentIds: List[Integer]) -> None:
        raise NotImplementedError()


class PriceListener:
    def newPrices(self, prices: List[Price]) -> None:
        raise NotImplementedError()

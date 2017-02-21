from enum import Enum
from typing import List, Any, cast

import yass
from tutorial.base_types_external import Integer


# shows how to use contract internal base types

class ExpirationHandler(yass.BaseTypeHandler):
    def readBase(self, reader):  # type: (yass.Reader) -> Expiration
        return Expiration(
            reader.readZigZagInt(),
            reader.readZigZagInt(),
            reader.readZigZagInt()
        )

    def writeBase(self, value, writer):  # type: (Expiration, yass.Writer) -> None
        writer.writeZigZagInt(value.year)
        writer.writeZigZagInt(value.month)
        writer.writeZigZagInt(value.day)


class Expiration:
    TYPE_DESC = yass.TypeDesc(yass.FIRST_DESC_ID + 1, ExpirationHandler())

    def __init__(self, year, month, day):  # type: (int, int, int) -> None
        self.year = year
        self.month = month
        self.day = day

    def __str__(self):
        return '%s-%s-%s' % (self.year, self.month, self.day)


class PriceKind(Enum):
    BID = 0
    ASK = 1


class Price:
    def __init__(self):  # type: () -> None
        self.instrumentId = cast('Integer', None)  # type: Integer
        self.kind = cast('PriceKind', None)  # type: PriceKind
        self.value = cast('Integer', None)  # type: Integer


@yass.abstract
class Instrument:
    def __init__(self):  # type: () -> None
        self.id = cast('Integer', None)  # type: Integer
        self.name = cast('unicode', None)  # type: unicode


class SystemException(Exception):
    def __init__(self):  # type: () -> None
        self.message = cast('unicode', None)  # type: unicode


@yass.abstract
class ApplicationException(Exception):
    def __init__(self):  # type: () -> None
        pass


class UnknownInstrumentsException(ApplicationException):
    def __init__(self):  # type: () -> None
        ApplicationException.__init__(self)
        self.instrumentIds = cast('List[Integer]', None)  # type: List[Integer]
        self.onlyNeededForTests1 = cast('Any', None)  # type: Any
        self.onlyNeededForTests2 = cast('bytes', None)  # type: bytes
        self.onlyNeededForTests3 = cast('Exception', None)  # type: Exception


class Node:
    def __init__(self):  # type: () -> None
        self.id = cast('float', None)  # type: float
        self.links = cast('List[Node]', None)  # type: List[Node]
        self.next = cast('Node', None)  # type: Node


class EchoService:
    def echo(self, value):  # type: (Any) -> Any
        raise NotImplementedError()


class PriceEngine:
    def subscribe(self, instrumentIds):  # type: (List[Integer]) -> None
        raise NotImplementedError()


class PriceListener:
    def newPrices(self, prices):  # type: (List[Price]) -> None
        raise NotImplementedError()

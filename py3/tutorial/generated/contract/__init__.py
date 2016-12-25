from enum import Enum
from typing import List, Any, cast

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
        return f"{self.year}-{self.month}-{self.day}"


class PriceKind(Enum):
    BID = 0
    ASK = 1


class Price:
    def __init__(self) -> None:
        self.instrumentId: Integer = cast(Integer, None)
        self.kind: PriceKind = cast(PriceKind, None)
        self.value: Integer = cast(Integer, None)


@yass.abstract
class Instrument:
    def __init__(self) -> None:
        self.id: Integer = cast(Integer, None)
        self.name: str = cast(str, None)


class SystemException(Exception):
    def __init__(self) -> None:
        self.message: str = cast(str, None)


@yass.abstract
class ApplicationException(Exception):
    def __init__(self) -> None:
        pass


class UnknownInstrumentsException(ApplicationException):
    def __init__(self) -> None:
        ApplicationException.__init__(self)
        self.instrumentIds: List[Integer] = cast(List[Integer], None)
        self.onlyNeededForTests1: Any = cast(Any, None)
        self.onlyNeededForTests2: bytes = cast(bytes, None)
        self.onlyNeededForTests3: Exception = cast(Exception, None)


class Node:
    def __init__(self) -> None:
        self.id: float = cast(float, None)
        self.links: List[Node] = cast(List[Node], None)


class EchoService:
    def echo(self, value: Any) -> Any:
        raise NotImplementedError()


class PriceEngine:
    def subscribe(self, instrumentIds: List[Integer]) -> None:
        raise NotImplementedError()


class PriceListener:
    def newPrices(self, prices: List[Price]) -> None:
        raise NotImplementedError()

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
        super().__init__()
        self.instrumentIds = None  # type: List[Integer]
        self.onlyNeededForTests1 = None  # type: Any
        self.onlyNeededForTests2 = None  # type: bytes
        self.onlyNeededForTests3 = None  # type: Exception


class Node:
    def __init__(self) -> None:
        self.id = None  # type: float
        self.links = None  # type: List[Node]


class EchoService:
    MAPPER = yass.MethodMapper([
        yass.MethodMapping(0, 'echo', False),
    ])

    def echo(self, value: Any) -> Any:
        raise NotImplementedError()


class PriceEngine:
    MAPPER = yass.MethodMapper([
        yass.MethodMapping(0, 'subscribe', False),
    ])

    def subscribe(self, instrumentIds: List[Integer]) -> None:
        raise NotImplementedError()


class PriceListener:
    MAPPER = yass.MethodMapper([
        yass.MethodMapping(0, 'newPrices', True),
    ])

    def newPrices(self, prices: List[Price]) -> None:
        raise NotImplementedError()


GENERATED_BY_YASS_VERSION = 'null'

from ..contract.instrument import stock as contract_instrument_stock
from ..contract import instrument as contract_instrument
from ..contract.instrument.stock import python as contract_instrument_stock_python

yass.enumDesc(9, PriceKind)
yass.classDesc(10, Price, False)
yass.classDesc(11, contract_instrument_stock.Stock, False)
yass.classDesc(12, contract_instrument.Bond, False)
yass.classDesc(13, SystemException, False)
yass.classDesc(14, UnknownInstrumentsException, False)
yass.classDesc(15, contract_instrument_stock_python.PythonBond, False)
yass.classDesc(16, contract_instrument_stock_python.PythonStock, False)
yass.classDesc(17, Node, True)

yass.fieldDescs(Price, [
    yass.FieldDesc(1, 'instrumentId', Integer.TYPE_DESC),
    yass.FieldDesc(2, 'kind', PriceKind),
    yass.FieldDesc(3, 'value', Integer.TYPE_DESC),
])
yass.fieldDescs(contract_instrument_stock.Stock, [
    yass.FieldDesc(1, 'paysDividend', yass.BOOLEAN_DESC),
    yass.FieldDesc(2, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(3, 'name', yass.STRING_DESC),
])
yass.fieldDescs(contract_instrument.Bond, [
    yass.FieldDesc(1, 'coupon', yass.DOUBLE_DESC),
    yass.FieldDesc(2, 'expiration', Expiration),
    yass.FieldDesc(3, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(4, 'name', yass.STRING_DESC),
])
yass.fieldDescs(SystemException, [
    yass.FieldDesc(1, 'message', yass.STRING_DESC),
])
yass.fieldDescs(UnknownInstrumentsException, [
    yass.FieldDesc(1, 'instrumentIds', yass.LIST_DESC),
    yass.FieldDesc(2, 'onlyNeededForTests1', None),
    yass.FieldDesc(3, 'onlyNeededForTests2', yass.BYTES_DESC),
    yass.FieldDesc(4, 'onlyNeededForTests3', None),
])
yass.fieldDescs(contract_instrument_stock_python.PythonBond, [
    yass.FieldDesc(1, 'coupon', yass.DOUBLE_DESC),
    yass.FieldDesc(2, 'expiration', Expiration),
    yass.FieldDesc(3, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(4, 'name', yass.STRING_DESC),
])
yass.fieldDescs(contract_instrument_stock_python.PythonStock, [
    yass.FieldDesc(1, 'paysDividend', yass.BOOLEAN_DESC),
    yass.FieldDesc(2, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(3, 'name', yass.STRING_DESC),
])
yass.fieldDescs(Node, [
    yass.FieldDesc(1, 'id', yass.DOUBLE_DESC),
    yass.FieldDesc(2, 'links', yass.LIST_DESC),
])

SERIALIZER = yass.FastSerializer([
    Integer.TYPE_DESC,
    Expiration,
    PriceKind,
    Price,
    contract_instrument_stock.Stock,
    contract_instrument.Bond,
    SystemException,
    UnknownInstrumentsException,
    contract_instrument_stock_python.PythonBond,
    contract_instrument_stock_python.PythonStock,
    Node,
])


class initiator:
    priceListener = yass.ContractId(PriceListener, 0)  # type: yass.ContractId[PriceListener]
    echoService = yass.ContractId(EchoService, 1)  # type: yass.ContractId[EchoService]


class acceptor:
    priceEngine = yass.ContractId(PriceEngine, 0)  # type: yass.ContractId[PriceEngine]
    instrumentService = yass.ContractId(contract_instrument.InstrumentService, 1)  # type: yass.ContractId[contract_instrument.InstrumentService]
    echoService = yass.ContractId(EchoService, 2)  # type: yass.ContractId[EchoService]

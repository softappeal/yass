import yass
from tutorial.base_types_external import Integer
from . import contract
from .contract.instrument import stock as contract_instrument_stock
from .contract import instrument as contract_instrument

yass.enumDesc(9, contract.PriceKind)
yass.classDesc(10, contract.Price, False)
yass.classDesc(11, contract_instrument_stock.Stock, False)
yass.classDesc(12, contract_instrument.Bond, False)
yass.classDesc(13, contract.SystemException, False)
yass.classDesc(14, contract.UnknownInstrumentsException, False)
yass.classDesc(15, contract.Node, True)

yass.fieldDescs(contract.Price, [
    yass.FieldDesc(1, 'instrumentId', Integer.TYPE_DESC),
    yass.FieldDesc(2, 'kind', contract.PriceKind),
    yass.FieldDesc(3, 'value', Integer.TYPE_DESC),
])
yass.fieldDescs(contract_instrument_stock.Stock, [
    yass.FieldDesc(1, 'paysDividend', yass.BOOLEAN_DESC),
    yass.FieldDesc(2, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(3, 'name', yass.STRING_DESC),
])
yass.fieldDescs(contract_instrument.Bond, [
    yass.FieldDesc(1, 'coupon', yass.DOUBLE_DESC),
    yass.FieldDesc(2, 'expiration', contract.Expiration),
    yass.FieldDesc(3, 'id', Integer.TYPE_DESC),
    yass.FieldDesc(4, 'name', yass.STRING_DESC),
])
yass.fieldDescs(contract.SystemException, [
    yass.FieldDesc(1, 'details', yass.STRING_DESC),
])
yass.fieldDescs(contract.UnknownInstrumentsException, [
    yass.FieldDesc(1, 'instrumentIds', yass.LIST_DESC),
    yass.FieldDesc(2, 'onlyNeededForTests1', None),
    yass.FieldDesc(3, 'onlyNeededForTests2', yass.BYTES_DESC),
    yass.FieldDesc(4, 'onlyNeededForTests3', None),
])
yass.fieldDescs(contract.Node, [
    yass.FieldDesc(1, 'id', yass.DOUBLE_DESC),
    yass.FieldDesc(2, 'links', yass.LIST_DESC),
    yass.FieldDesc(3, 'next', None),
])

SERIALIZER = yass.FastSerializer([
    Integer.TYPE_DESC,
    contract.Expiration,
    contract.PriceKind,
    contract.Price,
    contract_instrument_stock.Stock,
    contract_instrument.Bond,
    contract.SystemException,
    contract.UnknownInstrumentsException,
    contract.Node,
])

yass.methodMapper(contract.EchoService, [
    yass.MethodMapping(0, 'echo', False),
])
yass.methodMapper(contract.PriceEngine, [
    yass.MethodMapping(0, 'subscribe', False),
])
yass.methodMapper(contract.PriceListener, [
    yass.MethodMapping(0, 'newPrices', True),
])
yass.methodMapper(contract_instrument.InstrumentService, [
    yass.MethodMapping(0, 'getInstruments', False),
    yass.MethodMapping(1, 'showOneWay', True),
])


class INITIATOR:
    priceListener = yass.ContractId(contract.PriceListener, 0)  # type: yass.ContractId[contract.PriceListener]
    echoService = yass.ContractId(contract.EchoService, 1)  # type: yass.ContractId[contract.EchoService]


class ACCEPTOR:
    priceEngine = yass.ContractId(contract.PriceEngine, 0)  # type: yass.ContractId[contract.PriceEngine]
    instrumentService = yass.ContractId(contract_instrument.InstrumentService, 1)  # type: yass.ContractId[contract_instrument.InstrumentService]
    echoService = yass.ContractId(contract.EchoService, 2)  # type: yass.ContractId[contract.EchoService]

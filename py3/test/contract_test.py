import unittest
from typing import Any

import yass
from tutorial.base_types_external import Integer
from tutorial.generated import contract as contract
from tutorial.generated.contract import instrument as contract_instrument
from tutorial.generated.contract.instrument import stock as contract_instrument_stock
from tutorial.generated.contract.instrument.stock import python as contract_instrument_stock_python


@yass.abstract
class NoInit:
    pass


class Test(unittest.TestCase):
    def testVersion(self):
        self.assertEqual(contract.GENERATED_BY_YASS_VERSION, 'null')

    def testInteger(self):
        i = Integer(123)
        self.assertEqual(yass.typeDesc(i).id, 7)
        self.assertEqual(i.value, 123)
        self.assertEqual(str(i), '123')

    def testExpiration(self):
        e = contract.Expiration(2017, 11, 30)
        self.assertEqual(yass.typeDesc(e).id, 8)
        self.assertEqual(e.year, 2017)
        self.assertEqual(e.month, 11)
        self.assertEqual(e.day, 30)
        self.assertEqual(str(e), '2017-11-30')

    def testClasses(self):
        pythonStock = contract_instrument_stock_python.PythonStock()
        pythonStock.name = "ABB"
        pythonBond = contract_instrument_stock_python.PythonBond()
        pythonBond.name = "ABB"
        bond = contract_instrument.Bond()
        bond.name = "ABB"
        bond.coupon = 1.5
        stock = contract_instrument_stock.Stock()
        stock.name = "ABB"
        stock.paysDividend = True
        node1 = contract.Node()
        self.assertEqual(yass.typeDesc(node1).id, 17)
        node1.id = 1.
        node2 = contract.Node()
        node2.id = 2.
        node2.links = [node1, node2]
        try:
            uie = contract.UnknownInstrumentsException()
            uie.instrumentIds = [Integer(333), Integer(444)]
            uie.onlyNeededForTests2 = b'abc'
            raise uie
        except contract.UnknownInstrumentsException as e:
            pass

    def testEnum(self):
        self.assertEqual(yass.typeDesc(contract.PriceKind.ASK).id, 9)
        self.assertEqual(str(contract.PriceKind.ASK), 'PriceKind.ASK')
        self.assertEqual(contract.PriceKind.BID.value, 0)
        self.assertEqual(contract.PriceKind.ASK.value, 1)
        self.assertEqual(contract.PriceKind.ASK.name, 'ASK')
        self.assertTrue(contract.PriceKind(0) is contract.PriceKind.BID)
        self.assertTrue(contract.PriceKind(1) is contract.PriceKind.ASK)
        '''
        self.assertEqual(
            str(contract.PriceKind.__members__),
            "OrderedDict([('BID', <PriceKind.BID: 0>), ('ASK', <PriceKind.ASK: 1>)])"
        )
        '''

    def testInterface(self):
        class EchoServiceImpl(contract.EchoService):
            def __init__(self) -> None:
                super().__init__()

            def echo(self, value: Any) -> Any:
                return value

        echoService = EchoServiceImpl()
        self.assertEqual(echoService.echo("Hello World!"), 'Hello World!')

    def testInstance(self):
        self.assertTrue(isinstance(False, bool))
        self.assertTrue(isinstance(True, bool))
        self.assertTrue(isinstance(False, int))  # note: therefore int test must be after bool test
        self.assertTrue(isinstance(True, int))  # note: therefore int test must be after bool test
        self.assertTrue(isinstance(0, int))
        self.assertTrue(isinstance(1, int))
        self.assertFalse(isinstance(1., int))
        self.assertFalse(isinstance(1, float))
        self.assertTrue(isinstance(1., float))
        self.assertTrue(isinstance(123.456e77, float))
        self.assertTrue(isinstance('', str))
        self.assertTrue(isinstance('a', str))
        self.assertTrue(isinstance('aaa', str))
        self.assertFalse(isinstance(b'abc', str))
        self.assertTrue(isinstance(b'abc', bytes))
        self.assertFalse(isinstance(b'abc', str))
        self.assertTrue(isinstance([], list))
        self.assertTrue(isinstance([True, 1], list))
        self.assertFalse(hasattr(1, yass.TYPE_DESC))
        self.assertTrue(hasattr(contract.PriceKind.ASK, yass.TYPE_DESC))
        self.assertTrue(hasattr(contract.Node(), yass.TYPE_DESC))

    def testAbstract(self):
        try:
            contract.Instrument()
            self.fail()
        except RuntimeError as e:
            self.assertEqual(
                str(e),
                "can't instantiate abstract <class 'tutorial.generated.contract.Instrument'>"
            )
        contract.UnknownInstrumentsException()
        try:
            contract.ApplicationException()
            self.fail()
        except RuntimeError as e:
            self.assertEqual(
                str(e),
                "can't instantiate abstract <class 'tutorial.generated.contract.ApplicationException'>"
            )
        contract_instrument.Bond()
        try:
            NoInit()
            self.fail()
        except RuntimeError as e:
            pass

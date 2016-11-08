import unittest
from typing import Any

import yass
from tutorial.base_types_external import Integer
from tutorial.generated import contract, GENERATED_BY_YASS_VERSION
from tutorial.generated.contract import UnknownInstrumentsException, ApplicationException, PriceKind, Node, EchoService, Expiration
from tutorial.generated.contract.instrument import Bond
from tutorial.generated.contract.instrument.stock import Stock
from tutorial.generated.contract.instrument.stock.python import PythonStock, PythonBond


@yass.abstract
class NoInit:
    pass


class Test(unittest.TestCase):
    def testVersion(self):
        self.assertEqual(GENERATED_BY_YASS_VERSION, 'null')

    def testInteger(self):
        i = Integer(123)
        self.assertEqual(yass.typeDesc(i).id, 7)
        self.assertEqual(i.value, 123)
        self.assertEqual(str(i), '123')
        self.assertEqual(unicode(i), u'123')

    def testExpiration(self):
        e = Expiration(2017, 11, 30)
        self.assertEqual(yass.typeDesc(e).id, 8)
        self.assertEqual(e.year, 2017)
        self.assertEqual(e.month, 11)
        self.assertEqual(e.day, 30)
        self.assertEqual(str(e), '2017-11-30')
        self.assertEqual(unicode(e), u'2017-11-30')

    def testClasses(self):
        pythonStock = PythonStock()
        pythonStock.name = u"ABB"
        pythonBond = PythonBond()
        pythonBond.name = u"ABB"
        bond = Bond()
        bond.name = u"ABB"
        bond.coupon = 1.5
        stock = Stock()
        stock.name = u"ABB"
        stock.paysDividend = True
        node1 = Node()
        self.assertEqual(yass.typeDesc(node1).id, 17)
        node1.id = 1.
        node2 = Node()
        node2.id = 2.
        node2.links = [node1, node2]
        try:
            uie = UnknownInstrumentsException()
            uie.instrumentIds = [Integer(333), Integer(444)]
            uie.onlyNeededForTests2 = b'abc'
            raise uie
        except UnknownInstrumentsException as e:
            pass

    def testEnum(self):
        self.assertEqual(yass.typeDesc(PriceKind.ASK).id, 9)
        self.assertEqual(str(PriceKind.ASK), 'PriceKind.ASK')
        self.assertEqual(PriceKind.BID.value, 0)
        self.assertEqual(PriceKind.ASK.value, 1)
        self.assertEqual(PriceKind.ASK.name, 'ASK')
        self.assertTrue(PriceKind(0) is PriceKind.BID)
        self.assertTrue(PriceKind(1) is PriceKind.ASK)

    def testInterface(self):
        class EchoServiceImpl(EchoService):
            def echo(self, value):  # type: (Any) -> Any
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
        self.assertTrue(isinstance(u'aaa', unicode))
        self.assertFalse(isinstance('aaa', unicode))
        self.assertFalse(isinstance(u'aaa', str))
        self.assertTrue(isinstance(b'abc', str))
        self.assertTrue(isinstance(b'abc', bytes))
        self.assertTrue(isinstance('abc', bytes))
        self.assertTrue(isinstance([], list))
        self.assertTrue(isinstance([True, 1], list))
        self.assertFalse(hasattr(1, yass.TYPE_DESC))
        self.assertTrue(hasattr(PriceKind.ASK, yass.TYPE_DESC))
        self.assertTrue(hasattr(Node(), yass.TYPE_DESC))

    def disabled_testAbstract(self):  # enable if abstract decorator works
        try:
            contract.Instrument()
            self.fail()
        except RuntimeError as e:
            self.assertEqual(
                str(e),
                "can't instantiate abstract <class 'tutorial.generated.contract.Instrument'>"
            )
        UnknownInstrumentsException()
        try:
            ApplicationException()
            self.fail()
        except RuntimeError as e:
            self.assertEqual(
                str(e),
                "can't instantiate abstract <class 'tutorial.generated.contract.ApplicationException'>"
            )
        Bond()
        try:
            NoInit()
            self.fail()
        except RuntimeError as e:
            pass

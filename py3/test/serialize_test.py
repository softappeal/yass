import unittest
from io import BytesIO
from typing import Any, List

from tutorial.generated import SERIALIZER
from tutorial.generated.contract import Expiration, PriceKind, Node, UnknownInstrumentsException
from tutorial.generated.contract.instrument.stock import Stock
from tutorial.socket_client import createObjects
from yass import Writer, Reader


def createWriter(io: BytesIO) -> Writer:
    def writeBytes(b: bytes) -> None:
        if io.write(b) != len(b):
            raise RuntimeError('IO error')

    return Writer(writeBytes)


def createReader(io: BytesIO) -> Reader:
    def readBytes(length: int) -> bytes:
        b = io.read(length)
        if len(b) != length:
            raise RuntimeError('IO error')
        return b

    return Reader(readBytes)


def fromJava(b: List[int]) -> bytes:
    return bytes([i if i >= 0 else i + 256 for i in b])


class Test(unittest.TestCase):
    def genericCheck(self, value: Any, b: List[int], write: str, read: str) -> None:
        io = BytesIO()
        writer = createWriter(io)
        getattr(writer, write)(value)
        io.seek(0)
        self.assertEqual(io.read(len(b)), fromJava(b))
        self.assertEqual(io.read(1), b'')

        io = BytesIO(fromJava(b))
        reader = createReader(io)
        self.assertEqual(getattr(reader, read)(), value)
        self.assertEqual(io.read(1), b'')

    def testVarInt(self):
        def check(i: int, b: List[int]) -> None:
            self.genericCheck(i, b, 'writeVarInt', 'readVarInt')

        check(12, [12])
        check(0, [0])
        check(128, [-128, 1])
        check(60000, [-32, -44, 3])
        check(60000000, [-128, -114, -50, 28])
        check(-1, [-1, -1, -1, -1, 15])
        check(-34, [-34, -1, -1, -1, 15])
        check(0x12345678, [-8, -84, -47, -111, 1])
        check(0xfedcba98 - 0x100000000, [-104, -11, -14, -10, 15])
        check(-2147483648, [-128, -128, -128, -128, 8])
        check(2147483647, [-1, -1, -1, -1, 7])

    def testInt(self):
        def check(i: int, b: List[int]) -> None:
            self.genericCheck(i, b, 'writeInt', 'readInt')

        check(0, [0, 0, 0, 0])
        check(1, [0, 0, 0, 1])
        check(256, [0, 0, 1, 0])
        check(0x01020304, [1, 2, 3, 4])
        check(-1, [-1, -1, -1, -1])
        check(-2, [-1, -1, -1, -2])

    def testDouble(self):
        def check(f: float, b: List[int]) -> None:
            self.genericCheck(f, b, 'writeDouble', 'readDouble')

        check(1.2345e-12, [61, 117, -73, -79, -104, -15, -92, 40])
        check(1.7976931348623157e+308, [127, -17, -1, -1, -1, -1, -1, -1])
        check(4.9e-324, [0, 0, 0, 0, 0, 0, 0, 1])
        check(float('-inf'), [-1, -16, 0, 0, 0, 0, 0, 0])
        check(float('inf'), [127, -16, 0, 0, 0, 0, 0, 0])
        # check(float('nan'), [127, -8, 0, 0, 0, 0, 0, 0])

    def testZigZagInt(self):
        def check(i: int, b: List[int]) -> None:
            self.genericCheck(i, b, 'writeZigZagInt', 'readZigZagInt')

        check(12, [24])
        check(0, [0])
        check(128, [-128, 2])
        check(60000, [-64, -87, 7])
        check(60000000, [-128, -100, -100, 57])
        check(-34, [67])
        check(0x12345678, [-16, -39, -94, -93, 2])
        check(0xfedcba98 - 0x100000000, [-49, -107, -102, 18, ])
        check(-2147483648, [-1, -1, -1, -1, 15])
        check(2147483647, [-2, -1, -1, -1, 15])

    def testUtf8(self):
        self.assertEqual(b'abc'.decode(), 'abc')
        self.assertEqual("abc".encode(), b'abc')

    def testSerializer(self):
        io = BytesIO()
        writer = createWriter(io)
        reader = createReader(io)
        SERIALIZER.write(createObjects(), writer)
        io.seek(0)
        b = io.read(1000)
        self.assertEqual(
            b,
            fromJava(
                [2, 15, 0, 3, 0, 3, 1, 7, -128, -119, 15, 7, -117, -56, 120, 4, 84, 79, 126, -85, -80, 59, 20, -52, 5,
                 5, 72, 101, 108, 108, 111, 5, 20, 62, 1, 18, 127, -62, -128, -56, -76, -33, -65, -32, -96, -128, -28,
                 -116, -95, -17, -65, -65, 60, 6, 5, 0, 127, -1, 10, -45, 8, -62, 31, 22, 58, 9, 1, 9, 0, 11, 1, 1, 2,
                 -10, 1, 3, 4, 89, 65, 83, 83, 0, 14, 1, 3, 7, 2, 7, 4, 7, 6, 0, 17, 1, 63, -16, 0, 0, 0, 0, 0, 0, 2, 2,
                 1, 0, 17, 1, 64, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0])
        )
        io.seek(0)
        result = SERIALIZER.read(reader)
        self.assertEqual(io.read(1), b'')
        self.assertTrue(len(result) == 15)
        self.assertTrue(result[0] is None)
        self.assertEqual(result[1], False)
        self.assertEqual(result[2], True)
        self.assertEqual(result[3].value, 123456)
        self.assertEqual(result[4].value, -987654)
        self.assertEqual(result[5], 1.34545e98)
        self.assertEqual(result[6], "Hello")
        self.assertEqual(result[7], ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<")
        self.assertEqual(result[8], bytes([0, 127, 256 - 1, 10, 256 - 45]))
        expiration = result[9]  # type: Expiration
        self.assertEqual(expiration.year, 2017)
        self.assertEqual(expiration.month, 11)
        self.assertEqual(expiration.day, 29)
        self.assertEqual(result[10], PriceKind.ASK)
        self.assertEqual(result[11], PriceKind.BID)
        stock = result[12]  # type: Stock
        self.assertEqual(stock.id.value, 123)
        self.assertEqual(stock.name, "YASS")
        self.assertEqual(stock.paysDividend, True)
        unknownInstrumentsException = result[13]  # type: UnknownInstrumentsException
        instrumentIds = unknownInstrumentsException.instrumentIds
        self.assertTrue(len(instrumentIds) == 3)
        self.assertEqual(instrumentIds[0].value, 1)
        self.assertEqual(instrumentIds[1].value, 2)
        self.assertEqual(instrumentIds[2].value, 3)
        self.assertTrue(unknownInstrumentsException.onlyNeededForTests1 is None)
        self.assertTrue(unknownInstrumentsException.onlyNeededForTests2 is None)
        self.assertTrue(unknownInstrumentsException.onlyNeededForTests3 is None)
        node1 = result[14]  # type: Node
        self.assertEqual(node1.id, 1.0)
        self.assertTrue(len(node1.links) == 2)
        self.assertTrue(node1.links[0] is node1)
        node2 = node1.links[1]
        self.assertEqual(node2.id, 2.0)
        self.assertTrue(len(node2.links) == 0)
        io.truncate(0)
        io.seek(0)
        unknownInstrumentsException.onlyNeededForTests1 = 3.14
        SERIALIZER.write(unknownInstrumentsException, writer)
        io.seek(0)
        unknownInstrumentsException = SERIALIZER.read(reader)
        self.assertEqual(io.read(1), b'')
        self.assertEqual(unknownInstrumentsException.onlyNeededForTests1, 3.14)
        try:
            SERIALIZER.write(123, writer)
            self.fail()
        except RuntimeError as e:
            self.assertEqual(
                str(e),
                "missing type <class 'int'> in serializer"
            )

import unittest
from typing import Callable, Any, Set

from tutorial.base_types_external import Integer
from tutorial.generated.contract import Expiration
from tutorial.socket_client import createObjects
from yass import Dumper


class MyDumper(Dumper):
    def __init__(self, compact: bool, referenceables: bool, concreteValueClasses: Set[Any]) -> None:
        super().__init__(compact, referenceables, concreteValueClasses)

    def dumpValueClass(self, value: Any, write: Callable[[str], None]) -> bool:
        if self.isConcreteValueClass(value) or (value.__class__ in {Integer, Expiration}):
            write(str(value))
            return True
        return False


RESULT_WITH_CYCLES_EXTENDED = '''[
    null
    False
    True
    123456
    -987654
    1.34545e+98
    "Hello"
    ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<"
    b'\\x00\\x7f\\xff\\n\\xd3'
    2017-11-29
    ASK
    BID
    Stock(
        id = 123
        name = "YASS"
        paysDividend = True
    )#0
    UnknownInstrumentsException(
        instrumentIds = [
            1
            2
            3
        ]
    )#1
    Node(
        id = 1.0
        links = [
            #2
            Node(
                id = 2.0
                links = [
                ]
            )#3
        ]
    )#2
]'''

RESULT_WITHOUT_CYCLES_EXTENDED = '''[
    null
    False
    True
    123456
    -987654
    1.34545e+98
    "Hello"
    ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<"
    b'\\x00\\x7f\\xff\\n\\xd3'
    2017-11-29
    ASK
    BID
    Stock(
        id = 123
        name = "YASS"
        paysDividend = True
    )
    UnknownInstrumentsException(
        instrumentIds = [
            1
            2
            3
        ]
    )
]'''

RESULT_WITH_CYCLES_COMPACT = '''[ null False True 123456 -987654 1.34545e+98 "Hello" ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<" b'\\x00\\x7f\\xff\\n\\xd3' 2017-11-29 ASK BID Stock( id=123 name="YASS" paysDividend=True )#0 UnknownInstrumentsException( instrumentIds=[ 1 2 3 ] )#1 Node( id=1.0 links=[ #2 Node( id=2.0 links=[ ] )#3 ] )#2 ]'''

RESULT_WITHOUT_CYCLES_COMPACT = '''[ null False True 123456 -987654 1.34545e+98 "Hello" ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<" b'\\x00\\x7f\\xff\\n\\xd3' 2017-11-29 ASK BID Stock( id=123 name="YASS" paysDividend=True ) UnknownInstrumentsException( instrumentIds=[ 1 2 3 ] ) ]'''


class Test(unittest.TestCase):
    def testWithCyclesExtended(self):
        self.assertEqual(MyDumper(False, True, {Integer, Expiration}).toString(createObjects()), RESULT_WITH_CYCLES_EXTENDED)

    def testWithoutCyclesExtended(self):
        self.assertEqual(MyDumper(False, False, set()).toString(createObjects(withCycles=False)), RESULT_WITHOUT_CYCLES_EXTENDED)

    def testWithCyclesCompact(self):
        self.assertEqual(MyDumper(True, True, {Integer, Expiration}).toString(createObjects()), RESULT_WITH_CYCLES_COMPACT)

    def testWithoutCyclesCompact(self):
        self.assertEqual(MyDumper(True, False, set()).toString(createObjects(withCycles=False)), RESULT_WITHOUT_CYCLES_COMPACT)

import socket
from functools import partial
from typing import Any, Optional, cast, List

from tutorial.base_types_external import Integer
from tutorial.generated.contract import PriceKind, Expiration, UnknownInstrumentsException, Node, SERIALIZER, acceptor, SystemException
from tutorial.generated.contract.instrument.stock import Stock
from yass import Client, Request, Reply, defaultClientTransport, MethodMapping, Invocation, Interceptor, ClientTransport, Stream


def createObjects() -> Any:
    stock = Stock()
    stock.id = Integer(123)
    stock.name = "YASS"
    stock.paysDividend = True

    unknownInstrumentsException = UnknownInstrumentsException()
    unknownInstrumentsException.instrumentIds = [Integer(1), Integer(2), Integer(3)]

    node1 = Node()
    node1.id = 1.0
    node2 = Node()
    node2.id = 2.0
    node2.links = []
    node1.links = [node1, node2]

    return [
        None,
        False,
        True,
        Integer(123456),
        Integer(-987654),
        1.34545e98,
        "Hello",
        ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<",
        bytes([0, 127, 256 - 1, 10, 256 - 45]),
        Expiration(2017, 11, 29),
        PriceKind.ASK,
        PriceKind.BID,
        stock,
        unknownInstrumentsException,
        node1
    ]


def printer(side: str, mapping: MethodMapping, arguments: List[Optional[Any]], invocation: Invocation) -> Optional[Any]:
    def out(type: str, s: str) -> None:
        if True:
            print("%s | %9s | %s" % (side, type, s))

    out("entry", "%s(%s)" % (mapping.method, arguments))
    try:
        result = invocation()
        out("exit", result)
        return result
    except Exception as e:
        out("exception", str(e.__class__))
        raise


clientPrinter = cast(Interceptor, partial(printer, 'client'))
serverPrinter = cast(Interceptor, partial(printer, 'server'))

serializer = SERIALIZER

address = ("localhost", 28947)


class SocketStream(Stream):
    def __init__(self, s: socket.socket) -> None:
        self.s = s
        self.out = b''

    def writeBytes(self, value: bytes) -> None:
        self.out += value

    def writeDone(self) -> None:
        self.s.sendall(self.out)

    def readBytes(self, length: int) -> bytes:
        buffer = b''
        while len(buffer) < length:
            chunk = self.s.recv(length - len(buffer))
            if len(chunk) == 0:
                raise RuntimeError("socket connection broken")
            buffer += chunk
        return buffer


def socketClient(transport: ClientTransport, address: Any) -> Client:
    class SocketClient(Client):
        def invoke(self, request: Request) -> Reply:
            s = socket.socket()
            try:
                s.connect(address)
                return transport.invoke(request, SocketStream(s))
            finally:
                s.close()

    return SocketClient()


if __name__ == "__main__":
    client = socketClient(defaultClientTransport(serializer), address)
    echoService = client.proxy(acceptor.echoService)
    instrumentService = client.proxy(acceptor.instrumentService, clientPrinter)
    print(echoService.echo("hello"))
    print(echoService.echo(createObjects()))
    try:
        echoService.echo("exception")
    except SystemException as e:
        print(e.message)
    big = bytes(1000000)
    if len(echoService.echo(big)) != len(big):
        raise RuntimeError()
    instrumentService.showOneWay(True, Integer(123))
    print(instrumentService.getInstruments())

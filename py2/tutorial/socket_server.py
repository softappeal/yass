import socket

from tutorial.generated import ACCEPTOR
from tutorial.generated.contract import EchoService, SystemException
from tutorial.generated.contract.instrument import InstrumentService
from tutorial.socket_client import serializer, address, serverPrinter, SocketStream
from yass import Server, defaultServerTransport


class EchoServiceImpl(EchoService):
    def echo(self, value):  # type: (Any) -> Any
        if value == u"exception":
            e = SystemException()
            e.message = u"exception"
            raise e
        return value


class InstrumentServiceImpl(InstrumentService):
    def getInstruments(self):  # type: () -> List[contract.Instrument]
        return []

    def showOneWay(self, testBoolean, testInt):  # type: (bool, Integer) -> None
        pass


def socketServer(transport, address, backlog):  # type: (ServerTransport, Any, int) -> None
    serverSocket = socket.socket()
    serverSocket.bind(address)
    serverSocket.listen(backlog)
    while True:
        s = serverSocket.accept()[0]
        try:
            transport.invoke(SocketStream(s))
        finally:
            s.close()


if __name__ == "__main__":
    print("started")
    socketServer(
        defaultServerTransport(
            serializer,
            Server([
                ACCEPTOR.echoService.service(EchoServiceImpl()),
                ACCEPTOR.instrumentService.service(InstrumentServiceImpl(), serverPrinter)
            ])
        ),
        address,
        5
    )

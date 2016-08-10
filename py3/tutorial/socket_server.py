import socket
from typing import Any, List

from tutorial.base_types_external import Integer
from tutorial.generated import contract
from tutorial.generated.contract import acceptor, EchoService, SystemException
from tutorial.generated.contract.instrument import InstrumentService
from tutorial.socket_client import serializer, address, serverPrinter, SocketStream
from yass import Server, defaultServerTransport, ServerTransport


class EchoServiceImpl(EchoService):
    def echo(self, value: Any) -> Any:
        if value == "exception":
            e = SystemException()
            e.message = "exception"
            raise e
        return value


class InstrumentServiceImpl(InstrumentService):
    def getInstruments(self) -> List[contract.Instrument]:
        return []

    def showOneWay(self, testBoolean: bool, testInt: Integer) -> None:
        pass


def socketServer(transport: ServerTransport, address: Any, backlog: int):
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
                acceptor.echoService.service(EchoServiceImpl()),
                acceptor.instrumentService.service(InstrumentServiceImpl(), serverPrinter)
            ])
        ),
        address,
        5
    )

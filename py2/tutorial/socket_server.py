import socket
from ssl import SSLContext, SSLSocket
from typing import Any, List, cast

from tutorial.base_types_external import Integer
from tutorial.generated import contract, ACCEPTOR
from tutorial.generated.contract import EchoService, SystemException
from tutorial.generated.contract.instrument import InstrumentService
from tutorial.socket_client import serializer, address, serverPrinter, SocketStream, sslContext
from yass import Server, defaultServerTransport, ServerTransport


class EchoServiceImpl(EchoService):
    def echo(self, value):  # type: (Any) -> Any
        if value == u"exception":
            e = SystemException()
            e.message = value
            raise e
        return value


class InstrumentServiceImpl(InstrumentService):
    def getInstruments(self):  # type: () -> List[contract.Instrument]
        return []

    def showOneWay(self, testBoolean, testInt):  # type: (bool, Integer) -> None
        pass


def socketServer(transport, address, backlog, sslContext):  # type: (ServerTransport, Any, int, SSLContext) -> None
    serverSocket = socket.socket()
    serverSocket.bind(address)
    serverSocket.listen(backlog)
    while True:
        s = cast(SSLSocket, sslContext.wrap_socket(serverSocket.accept()[0], server_side=True))
        try:
            print(s.getpeercert())
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
        5,
        sslContext("TestCA.cert.pem", "Server.key.pem")
    )

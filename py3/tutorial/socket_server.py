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
    def echo(self, value: Any) -> Any:
        if value == "exception":
            e = SystemException()
            e.details = "exception"
            raise e
        return value


class InstrumentServiceImpl(InstrumentService):
    def getInstruments(self) -> List[contract.Instrument]:
        return []

    def showOneWay(self, testBoolean: bool, testInt: Integer) -> None:
        pass


def socketServer(transport: ServerTransport, address: Any, backlog: int, sslContext: SSLContext) -> None:
    serverSocket = socket.socket()
    serverSocket.bind(address)
    serverSocket.listen(backlog)
    while True:
        try:
            s = cast(SSLSocket, sslContext.wrap_socket(serverSocket.accept()[0], server_side=True))
            try:
                print(s.getpeercert())
                transport.invoke(SocketStream(s))
            finally:
                s.close()
        except Exception as e:
            print("exception:", e)


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
        sslContext("ClientCA.cert.pem", "Server.key.pem")
    )

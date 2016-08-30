from __future__ import print_function

import sys

from typing import cast

from tutorial.generated import ACCEPTOR
from tutorial.socket_client import serializer
from tutorial.socket_server import EchoServiceImpl, InstrumentServiceImpl
from yass import Server, Writer, MessageSerializer, Reader, Request, Serializer


def stdServer(server, contractSerializer):  # type: (Server, Serializer) -> None
    sout = sys.stdout
    sin = sys.stdin

    def writeBytes(value):  # type: (bytes) -> None
        sout.write(value)

    def readBytes(length):  # type: (int) -> bytes
        buffer = b''
        while len(buffer) < length:
            chunk = sin.read(length - len(buffer))
            if len(chunk) == 0:
                raise RuntimeError("socket connection broken")
            buffer += chunk
        return buffer

    messageSerializer = MessageSerializer(contractSerializer)
    writer = Writer(writeBytes)
    reader = Reader(readBytes)
    while True:
        messageSerializer.write(server.invoke(cast(Request, messageSerializer.read(reader))), writer)
        sout.flush()


print('server started', file=sys.stderr)

stdServer(
    Server([
        ACCEPTOR.echoService.service(EchoServiceImpl()),
        ACCEPTOR.instrumentService.service(InstrumentServiceImpl())
    ]),
    serializer
)

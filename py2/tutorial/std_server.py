# todo: todo: does not yet work

import os
import sys

from typing import cast

from tutorial.generated import ACCEPTOR
from tutorial.socket_client import serializer
from tutorial.socket_server import EchoServiceImpl, InstrumentServiceImpl
from yass import Server, Writer, MessageSerializer, Reader, Request, Serializer

if sys.platform == "win32":
    import msvcrt

    msvcrt.setmode(sys.stdout.fileno(), os.O_BINARY)
    msvcrt.setmode(sys.stdin.fileno(), os.O_BINARY)


def stdServer(server, contractSerializer):  # type: (Server, Serializer) -> None
    sout = sys.stdout.buffer
    sin = sys.stdin.buffer

    def writeBytes(value):  # type: (bytes) -> None
        sent = 0
        while sent < len(value):
            written = sout.write(value[sent:])
            if written == 0:
                raise RuntimeError("pipe connection broken")
            sent += written

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


stdServer(
    Server([
        ACCEPTOR.echoService.service(EchoServiceImpl()),
        ACCEPTOR.instrumentService.service(InstrumentServiceImpl())
    ]),
    serializer
)

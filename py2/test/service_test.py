import unittest
from io import BytesIO
from typing import Any, cast

from test.serialize_test import createWriter, createReader
from tutorial.base_types_external import Integer
from tutorial.generated import ACCEPTOR
from tutorial.generated.contract import SystemException
from tutorial.socket_client import serializer
from tutorial.socket_server import EchoServiceImpl, InstrumentServiceImpl
from yass import Server, Client, Request, Reply, MessageSerializer

echo_service = ACCEPTOR.echoService.service(EchoServiceImpl())
instrument_service = ACCEPTOR.instrumentService.service(InstrumentServiceImpl())
server = Server([echo_service, instrument_service])


class Test(unittest.TestCase):
    def testInvalidService(self):
        try:
            server.invoke(Request(123, 321, []))
            self.fail()
        except RuntimeError as e:
            self.assertEqual(str(e), "no serviceId 123 found")

    def testInvalidMethod(self):
        try:
            echo_service.invoke(Request(123, 321, []))
            self.fail()
        except RuntimeError as e:
            self.assertEqual(str(e), "no methodId 321 found for serviceId 123")

    def testDuplicatedService(self):
        try:
            Server([echo_service, echo_service])
            self.fail()
        except RuntimeError as e:
            self.assertEqual(str(e), "serviceId 2 already added")

    def test(self):
        test = self

        class LocalClient(Client):
            def invoke(self, request):  # type: (Request) -> Reply
                ser = MessageSerializer(serializer)
                io = BytesIO()
                writer = createWriter(io)
                reader = createReader(io)

                def copy(input):  # type: (Any) -> Any
                    io.truncate(0)
                    io.seek(0)
                    ser.write(input, writer)
                    io.seek(0)
                    output = ser.read(reader)
                    test.assertEqual(io.read(1), b'')
                    return output

                return cast(Reply, copy(server.invoke(cast(Request, copy(request)))))

        client = LocalClient()

        try:
            cast(Any, client.proxy(ACCEPTOR.echoService)).xxx()
            self.fail()
        except RuntimeError as e:
            self.assertEqual(str(e), "no method 'xxx' found for serviceId 2")

        echoService = client.proxy(ACCEPTOR.echoService)
        instrumentService = client.proxy(ACCEPTOR.instrumentService)
        self.assertEqual(echoService.echo(u"world"), u"world")
        try:
            echoService.echo(u"exception")
            self.fail()
        except SystemException as e:
            self.assertEqual(e.message, u"exception")
        instrumentService.showOneWay(True, Integer(2))
        self.assertEqual(instrumentService.getInstruments(), [])

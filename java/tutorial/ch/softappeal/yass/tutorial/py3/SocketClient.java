package ch.softappeal.yass.tutorial.py3;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketConnector;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.tutorial.acceptor.socket.SocketAcceptor;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.Node;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.Arrays;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public final class SocketClient {

    static Object createObjects() {
        final Node node1 = new Node(1.0);
        final Node node2 = new Node(2.0);
        node1.links.add(node1);
        node1.links.add(node2);
        return Arrays.asList(
            null,
            false,
            true,
            123456,
            -987654,
            1.34545e98d,
            "Hello",
            ">\u0001\u0012\u007F\u0080\u0234\u07FF\u0800\u4321\uFFFF<",
            new byte[] {0, 127, -1, 10, -45},
            new Expiration(2017, 11, 29),
            PriceKind.ASK,
            PriceKind.BID,
            new Stock(123, "YASS", true),
            new UnknownInstrumentsException(Arrays.asList(1, 2, 3)),
            node1
        );
    }

    static final Serializer SERIALIZER = Config.PY3_CONTRACT_SERIALIZER;

    static void client(final Client client) {
        final Interceptor logger = new Logger(null, Logger.Side.CLIENT);
        final EchoService echoService = client.proxy(ACCEPTOR.echoService);
        final InstrumentService instrumentService = client.proxy(ACCEPTOR.instrumentService, logger);
        System.out.println(echoService.echo("hello"));
        System.out.println(echoService.echo(createObjects()));
        try {
            echoService.echo("exception");
        } catch (final SystemException e) {
            System.out.println(e.message);
        }
        final byte[] big = new byte[1_000_000];
        if (((byte[])echoService.echo(big)).length != big.length) {
            throw new RuntimeException();
        }
        instrumentService.showOneWay(true, 123);
        System.out.println(instrumentService.getInstruments());
    }

    public static void main(final String... args) {
        client(SimpleSocketTransport.client(new MessageSerializer(SERIALIZER), new SimpleSocketConnector(SocketAcceptor.ADDRESS)));
    }

}

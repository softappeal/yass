package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.ClientSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Expiration;
import ch.softappeal.yass.tutorial.contract.Node;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.SslConfig;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static ch.softappeal.yass.transport.MessageSerializerKt.messageSerializer;
import static ch.softappeal.yass.transport.socket.SocketKt.socketConnector;
import static ch.softappeal.yass.transport.socket.SocketTransportKt.socketClient;
import static ch.softappeal.yass.transport.socket.SslSetupKt.getSocketFactory;
import static ch.softappeal.yass.tutorial.contract.Config.PY_ACCEPTOR;

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
            new byte[]{0, 127, -1, 10, -45},
            new Expiration(2017, 11, 29),
            PriceKind.ASK,
            PriceKind.BID,
            new Stock(123, "YASS", true),
            new UnknownInstrumentsException(Arrays.asList(1, 2, 3)),
            node1
        );
    }

    static final Serializer SERIALIZER = Config.PY_CONTRACT_SERIALIZER;

    static void client(final Client client) {
        final Function3<Method, List<?>, Function0<?>, Object> logger = new Logger(null, Logger.Side.CLIENT);
        final EchoService echoService = client.proxy(PY_ACCEPTOR.echoService);
        final InstrumentService instrumentService = client.proxy(PY_ACCEPTOR.instrumentService, logger);
        System.out.println(echoService.echo("hello"));
        System.out.println(echoService.echo(createObjects()));
        try {
            echoService.echo("exception");
        } catch (final SystemException e) {
            System.out.println(e.details);
        }
        final byte[] big = new byte[1_000_000];
        if (((byte[]) echoService.echo(big)).length != big.length) {
            throw new RuntimeException();
        }
        instrumentService.showOneWay(true, 123);
        System.out.println(instrumentService.getInstruments());
    }

    public static void main(final String... args) {
        client(socketClient(
            new ClientSetup(messageSerializer(SERIALIZER)),
            socketConnector(SocketSetup.ADDRESS, getSocketFactory(SslConfig.CLIENT))
        ));
    }

}

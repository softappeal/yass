package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.contract.instrument.*;
import ch.softappeal.yass.tutorial.shared.*;
import ch.softappeal.yass.tutorial.shared.socket.*;
import kotlin.jvm.functions.*;

import javax.net.ssl.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import static ch.softappeal.yass.ThreadFactoryKt.*;
import static ch.softappeal.yass.remote.ServerKt.*;
import static ch.softappeal.yass.transport.MessageSerializerKt.*;
import static ch.softappeal.yass.transport.socket.SocketKt.*;
import static ch.softappeal.yass.transport.socket.SocketTransportKt.*;
import static ch.softappeal.yass.transport.socket.SslSetupKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class SocketServer {

    private static final class EchoServiceImpl implements EchoService {
        @Override
        public Object echo(final Object value) {
            if ("exception".equals(value)) {
                throw new SystemException("exception");
            }
            return value;
        }
    }

    private static final class InstrumentServiceImpl implements InstrumentService {
        @Override
        public List<Instrument> getInstruments() {
            return Collections.emptyList();
        }

        @Override
        public void showOneWay(final boolean testBoolean, final int testInt) {
            // empty
        }
    }

    private static final Function3<Method, List<?>, Function0<?>, Object> LOGGER =
        new Logger(null, Logger.Side.SERVER);

    private static final Function3<Method, List<?>, Function0<?>, Object> PEER = (method, arguments, invocation) -> {
        try {
            System.out.println(((SSLSocket) getSocket()).getSession().getPeerPrincipal().getName());
        } catch (final SSLPeerUnverifiedException e) {
            throw new RuntimeException(e);
        }
        return invocation.invoke();
    };

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketServer(
            new ServerSetup(
                new Server(
                    service(PY_ACCEPTOR.echoService, new EchoServiceImpl()),
                    service(PY_ACCEPTOR.instrumentService, new InstrumentServiceImpl(), PEER, LOGGER)
                ),
                messageSerializer(SocketClient.SERIALIZER)
            ),
            executor
        ).start(
            executor,
            socketBinder(SocketSetup.ADDRESS, getServerSocketFactory(SslConfig.SERVER))
        );
        System.out.println("started");
    }

}

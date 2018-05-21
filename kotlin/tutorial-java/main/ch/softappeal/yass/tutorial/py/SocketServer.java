package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.transport.ServerSetup;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.SslConfig;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.Kt.getStdErr;
import static ch.softappeal.yass.Kt.namedThreadFactory;
import static ch.softappeal.yass.remote.Kt.service;
import static ch.softappeal.yass.transport.Kt.messageSerializer;
import static ch.softappeal.yass.transport.socket.Kt.socketBinder;
import static ch.softappeal.yass.transport.socket.Kt.socket;
import static ch.softappeal.yass.transport.socket.Kt.socketServer;
import static ch.softappeal.yass.transport.socket.Kt.serverSocketFactory;
import static ch.softappeal.yass.tutorial.contract.Config.PY_ACCEPTOR;

public final class SocketServer {

    private static final class EchoServiceImpl implements EchoService {
        @Override public Object echo(final Object value) {
            if ("exception".equals(value)) {
                throw new SystemException("exception");
            }
            return value;
        }
    }

    private static final class InstrumentServiceImpl implements InstrumentService {
        @Override public List<Instrument> getInstruments() {
            return Collections.emptyList();
        }
        @Override public void showOneWay(final boolean testBoolean, final int testInt) {
            // empty
        }
    }

    private static final Function3<Method, List<?>, Function0<?>, Object> LOGGER = new Logger(null, Logger.Side.SERVER);

    private static final Function3<Method, List<?>, Function0<?>, Object> PEER = (method, arguments, invocation) -> {
        try {
            System.out.println(((SSLSocket)socket()).getSession().getPeerPrincipal().getName());
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
            socketBinder(SocketSetup.ADDRESS, serverSocketFactory(SslConfig.SERVER))
        );
        System.out.println("started");
    }

}

package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.transport.socket.SocketBinder;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.SslConfig;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import javax.net.ssl.SSLSocket;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.tutorial.contract.Config.PY_ACCEPTOR;

public final class SocketServer {

    private static final class EchoServiceImpl implements EchoService {
        @Override public @Nullable Object echo(final @Nullable Object value) {
            if ("exception".equals(value)) {
                throw new SystemException("exception");
            }
            return value;
        }
    }

    private static final class InstrumentServiceImpl implements InstrumentService {
        @Override public List<Instrument> getInstruments() {
            return List.of();
        }
        @Override public void showOneWay(final boolean testBoolean, final int testInt) {
            // empty
        }
    }

    private static final Interceptor LOGGER = new Logger(null, Logger.Side.SERVER);

    private static final Interceptor PEER = (method, arguments, invocation) -> {
        System.out.println(((SSLSocket)SimpleSocketTransport.socket()).getSession().getPeerPrincipal().getName());
        return invocation.proceed();
    };

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        new SimpleSocketTransport(
            executor,
            new MessageSerializer(SocketClient.SERIALIZER),
            new Server(
                new Service(PY_ACCEPTOR.echoService, new EchoServiceImpl()),
                new Service(PY_ACCEPTOR.instrumentService, new InstrumentServiceImpl(), PEER, LOGGER)
            )
        ).start(
            executor,
            SocketBinder.create(SslConfig.SERVER.serverSocketFactory, SocketSetup.ADDRESS)
        );
        System.out.println("started");
    }

}

package ch.softappeal.yass.tutorial.py3;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SimpleSocketBinder;
import ch.softappeal.yass.transport.socket.SimpleSocketTransport;
import ch.softappeal.yass.tutorial.acceptor.socket.SocketAcceptor;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

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
            return new ArrayList<>();
        }
        @Override public void showOneWay(final boolean testBoolean, final int testInt) {
            // empty
        }
    }

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        final Interceptor logger = new Logger(null, Logger.Side.SERVER);
        new SimpleSocketTransport(
            executor,
            new MessageSerializer(SocketClient.SERIALIZER),
            new ch.softappeal.yass.core.remote.Server(
                ACCEPTOR.echoService.service(new EchoServiceImpl()),
                ACCEPTOR.instrumentService.service(new InstrumentServiceImpl(), logger)
            )
        ).start(executor, new SimpleSocketBinder(SocketAcceptor.ADDRESS));
        System.out.println("started");
    }

}

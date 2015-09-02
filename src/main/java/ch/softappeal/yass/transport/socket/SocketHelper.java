package ch.softappeal.yass.transport.socket;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.DummyPathSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SocketHelper {

    public static ExecutorService newExecutorService() {
        return Executors.newCachedThreadPool(new NamedThreadFactory("SocketHelper.Executor", Exceptions.STD_ERR));
    }

    private MethodMapper.Factory methodMapperFactory;

    public SocketHelper methodMapperFactory(final MethodMapper.Factory methodMapperFactory) {
        this.methodMapperFactory = Check.notNull(methodMapperFactory);
        return this;
    }

    private Serializer packetSerializer;

    public SocketHelper packetSerializer(final Serializer packetSerializer) {
        this.packetSerializer = Check.notNull(packetSerializer);
        return this;
    }

    private final List<Service> services = new ArrayList<>();

    public <C> SocketHelper addService(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
        services.add(new Service(contractId, implementation, interceptors));
        return this;
    }

    private SessionFactory sessionFactory;

    public SocketHelper sessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = Check.notNull(sessionFactory);
        return this;
    }

    private static void handleCloseException(@Nullable final Throwable throwable) {
        if (throwable != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, throwable);
        }
    }

    @FunctionalInterface public interface Opened {
        void opened(Session session) throws Exception;
    }

    /**
     * Calls {@link #sessionFactory(SessionFactory)} with an adaptor for {@link Opened}.
     */
    public SocketHelper opened(final Opened opened) {
        return sessionFactory(sessionClient -> new Session(sessionClient) {
            @Override protected void opened() throws Exception {
                opened.opened(this);
            }
            @Override protected void closed(@Nullable final Throwable throwable) {
                handleCloseException(throwable);
            }
        });
    }

    private final Executor executor;

    /**
     * Calls {@link #methodMapperFactory(MethodMapper.Factory)} with {@link SimpleMethodMapper#FACTORY}.
     * Calls {@link #packetSerializer(Serializer)} with {@link JavaSerializer#INSTANCE}.
     * Calls {@link #sessionFactory(SessionFactory)} with an 'empty' session factory.
     */
    public SocketHelper(final Executor executor) {
        this.executor = Check.notNull(executor);
        methodMapperFactory(SimpleMethodMapper.FACTORY);
        packetSerializer(JavaSerializer.INSTANCE);
        sessionFactory(sessionClient -> new Session(sessionClient) {
            @Override protected void closed(@Nullable final Throwable throwable) {
                handleCloseException(throwable);
            }
        });
    }

    private TransportSetup transportSetup() {
        return new TransportSetup(new Server(methodMapperFactory, services.toArray(new Service[0])), executor, packetSerializer, sessionFactory);
    }

    public void connect(final SocketFactory socketFactory, final SocketAddress socketAddress) {
        SocketTransport.connect(transportSetup(), executor, DummyPathSerializer.INSTANCE, DummyPathSerializer.PATH, socketFactory, socketAddress);
    }

    public void connect(final SocketAddress socketAddress) {
        connect(SocketFactory.getDefault(), socketAddress);
    }

    public void start(final ServerSocketFactory socketFactory, final SocketAddress socketAddress) {
        SocketTransport.listener(transportSetup()).start(executor, executor, socketFactory, socketAddress);
    }

    public void start(final SocketAddress socketAddress) {
        start(ServerSocketFactory.getDefault(), socketAddress);
    }

}

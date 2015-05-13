package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.util.ContextLocator;

import java.util.concurrent.Executor;

public abstract class ServerSetup {

    public static final Server SERVER = new Server(
        Config.METHOD_MAPPER_FACTORY,
        new Service(ServerServices.InstrumentService, new InstrumentServiceImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER),
        new Service(
            ServerServices.PriceEngine,
            new PriceEngineImpl(InstrumentServiceImpl.INSTRUMENTS, new ContextLocator<PriceEngineContext>() {
                @Override public PriceEngineContext context() {
                    return (PriceEngineContext)Session.get();
                }
            }),
            UnexpectedExceptionHandler.INSTANCE,
            Logger.SERVER
        ),
        new Service(ServerServices.EchoService, new EchoServiceImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER)
    );

    protected static TransportSetup createTransportSetup(final Executor dispatcherExecutor) {
        return new TransportSetup(SERVER, dispatcherExecutor, Config.PACKET_SERIALIZER, new SessionFactory() {
            @Override public Session create(final SessionClient sessionClient) throws Exception {
                return new ServerSession(sessionClient);
            }
        });
    }

}

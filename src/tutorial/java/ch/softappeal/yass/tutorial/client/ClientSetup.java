package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;

import java.util.concurrent.Executor;

public abstract class ClientSetup {

    public static final Server SERVER = new Server(
        Config.METHOD_MAPPER_FACTORY,
        new Service(ClientServices.PriceListener, new PriceListenerImpl(() -> (PriceListenerContext)Session.get())),
        new Service(ClientServices.EchoService, new EchoServiceImpl())
    );

    protected static TransportSetup createTransportSetup(final Executor requestExecutor, final SessionFactory sessionFactory) {
        return new TransportSetup(SERVER, requestExecutor, Config.PACKET_SERIALIZER, sessionFactory);
    }

}

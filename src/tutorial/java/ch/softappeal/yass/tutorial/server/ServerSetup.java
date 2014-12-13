package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.ServerServices;

import java.util.concurrent.Executor;

public abstract class ServerSetup {

    protected static TransportSetup createTransportSetup(final Executor requestExecutor) {
        return new TransportSetup(
            new Server(
                Config.METHOD_MAPPER_FACTORY,
                new Service(ServerServices.InstrumentService, new InstrumentServiceImpl(), Logger.SERVER),
                new Service(ServerServices.PriceEngine, new PriceEngineImpl(() -> (PriceEngineContext)Session.get()), Logger.SERVER),
                new Service(ServerServices.EchoService, new EchoServiceImpl())
            ),
            requestExecutor,
            Config.PACKET_SERIALIZER,
            ServerSession::new
        );
    }

}

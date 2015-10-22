package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;

import java.util.concurrent.Executor;

public abstract class ClientSetup {

    public static final Server SERVER = new Server(
        Config.METHOD_MAPPER_FACTORY,
        new Service(ClientServices.PriceListener, new PriceListenerImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER),
        new Service(ClientServices.EchoService, new EchoServiceImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER)
    );

    protected static TransportSetup createTransportSetup(final Executor dispatcherExecutor) {
        return TransportSetup.create(SERVER, dispatcherExecutor, Config.SERIALIZER, ClientSession::new);
    }

}

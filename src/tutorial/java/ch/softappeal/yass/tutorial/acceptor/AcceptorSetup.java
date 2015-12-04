package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.AcceptorServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;

import java.util.concurrent.Executor;

public abstract class AcceptorSetup {

    private static final PriceEngine PRICE_ENGINE = new PriceEngineImpl(
        InstrumentServiceImpl.INSTRUMENTS,
        () -> ((AcceptorSession)Session.get()).subscribedInstrumentIds
    );

    public static final Server SERVER = new Server(
        Config.METHOD_MAPPER_FACTORY,
        new Service(AcceptorServices.InstrumentService, new InstrumentServiceImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER),
        new Service(AcceptorServices.PriceEngine, PRICE_ENGINE, UnexpectedExceptionHandler.INSTANCE, Logger.SERVER),
        new Service(AcceptorServices.EchoService, new EchoServiceImpl(), UnexpectedExceptionHandler.INSTANCE, Logger.SERVER)
    );

    protected static TransportSetup createTransportSetup(final Executor dispatcherExecutor) {
        return TransportSetup.create(SERVER, dispatcherExecutor, Config.SERIALIZER, AcceptorSession::new);
    }

}

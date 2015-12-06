package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.tutorial.contract.AcceptorServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.InitiatorServices;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class InitiatorSession extends SimpleSession {

    @Override protected Server server() {
        final Interceptor interceptor = Interceptor.composite(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            Config.METHOD_MAPPER_FACTORY,
            new Service(InitiatorServices.PriceListener, new PriceListenerImpl(), interceptor),
            new Service(InitiatorServices.EchoService, new EchoServiceImpl(), interceptor)
        );
    }

    private final PriceEngine priceEngine;
    private final InstrumentService instrumentService;
    private final EchoService echoService;

    public InitiatorSession(final Connection connection, final Executor dispatchExecutor) {
        super(Config.METHOD_MAPPER_FACTORY, connection, dispatchExecutor);
        System.out.println("session " + this + " created");
        final Interceptor interceptor = new Logger(this, Logger.Side.CLIENT);
        priceEngine = proxy(AcceptorServices.PriceEngine, interceptor);
        instrumentService = proxy(AcceptorServices.InstrumentService, interceptor);
        echoService = proxy(AcceptorServices.EchoService, interceptor);
    }

    @Override protected void opened() throws UnknownInstrumentsException {
        System.out.println("session " + this + " opened");
        System.out.println("echo: " + echoService.echo("hello from initiator"));
        try {
            echoService.echo("throwRuntimeException");
        } catch (final SystemException ignore) {
            ignore.printStackTrace(System.out);
        }
        try {
            priceEngine.subscribe(Arrays.asList(123456789, 987654321));
        } catch (final UnknownInstrumentsException ignore) {
            ignore.printStackTrace(System.out);
        }
        instrumentService.reload(false, 123);
        priceEngine.subscribe(
            instrumentService
                .getInstruments()
                .stream()
                .map(instrument -> instrument.id)
                .collect(Collectors.toList())
        );
    }

    @Override protected void closed(final @Nullable Exception exception) {
        System.out.println("session " + this + " closed");
        if (exception != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, exception);
        }
    }

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String id = String.valueOf(ID.getAndIncrement());
    @Override public String toString() {
        return id;
    }

}

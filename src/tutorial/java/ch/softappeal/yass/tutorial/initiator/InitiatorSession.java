package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.tutorial.contract.AcceptorServices;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class InitiatorSession extends Session {

    private final PriceEngine priceEngine;
    private final InstrumentService instrumentService;
    private final EchoService echoService;

    public InitiatorSession(final SessionClient sessionClient) {
        super(sessionClient);
        System.out.println("session " + this + " created");
        priceEngine = proxy(AcceptorServices.PriceEngine, Logger.CLIENT);
        instrumentService = proxy(AcceptorServices.InstrumentService, Logger.CLIENT);
        echoService = proxy(AcceptorServices.EchoService, Logger.CLIENT);
    }

    @Override public void opened() throws UnknownInstrumentsException {
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
        priceEngine.subscribe(instrumentService.getInstruments().stream().map(instrument -> instrument.id).collect(Collectors.toList()));
    }

    @Override public void closed(final @Nullable Throwable throwable) {
        System.out.println("session " + this + " closed");
        if (throwable != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, throwable);
        }
    }

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String id = String.valueOf(ID.getAndIncrement());
    @Override public String toString() {
        return id;
    }

}

package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientSession extends Session {
    
    private final PriceEngine priceEngine;
    private final InstrumentService instrumentService;
    private final EchoService echoService;

    public ClientSession(final SessionClient sessionClient) {
        super(sessionClient);
        System.out.println("session " + this + " created");
        priceEngine = proxy(ServerServices.PriceEngine, Logger.CLIENT);
        instrumentService = proxy(ServerServices.InstrumentService, Logger.CLIENT);
        echoService = proxy(ServerServices.EchoService, Logger.CLIENT);
    }

    @Override public void opened() throws UnknownInstrumentsException {
        System.out.println("session " + this + " opened");
        System.out.println("echo: " + echoService.echo("hello from client"));
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
        final Map<Integer, Instrument> id2instrument = Collections.synchronizedMap(new HashMap<Integer, Instrument>());
        for (final Instrument instrument : instrumentService.getInstruments()) {
            id2instrument.put(instrument.id, instrument);
        }
        priceEngine.subscribe(new ArrayList<>(id2instrument.keySet()));
    }

    @Override public void closed(@Nullable final Throwable throwable) {
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

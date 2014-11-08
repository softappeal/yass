package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClientSession extends Session implements PriceListenerContext {

    private final Map<String, Instrument> id2instrument = Collections.synchronizedMap(new HashMap<>());

    private final PriceEngine priceEngine;
    private final InstrumentService instrumentService;
    private final EchoService echoService;

    public ClientSession(final SessionClient sessionClient) {
        super(sessionClient);
        System.out.println("create: " + hashCode());
        priceEngine = invoker(ServerServices.PriceEngine).proxy();
        instrumentService = invoker(ServerServices.InstrumentService).proxy();
        echoService = invoker(ServerServices.EchoService).proxy();
    }

    @Override public void opened() throws UnknownInstrumentsException {
        System.out.println("opened: " + hashCode());
        System.out.println(echoService.echo("echo"));
        instrumentService.reload(false, 123);
        instrumentService.getInstruments().forEach(instrument -> id2instrument.put(instrument.id, instrument));
        priceEngine.subscribe(new ArrayList<>(id2instrument.keySet()));
    }

    @Override public void closed(@Nullable final Throwable throwable) {
        System.out.println("closed: " + hashCode());
        if (throwable != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, throwable);
        }
    }

    @Override public Instrument getInstrument(final String instrumentId) {
        return id2instrument.get(instrumentId);
    }

}

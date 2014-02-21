package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.InstrumentService;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ClientSession extends Session implements PriceListenerContext {

  private final PriceEngine priceEngine;
  private final InstrumentService instrumentService;

  public ClientSession(final SessionSetup setup, final Connection connection) {
    super(setup, connection);
    System.out.println("create: " + hashCode());
    priceEngine = ServerServices.PriceEngine.invoker(this).proxy();
    instrumentService = ServerServices.InstrumentService.invoker(this).proxy();
  }

  private final Map<String, Instrument> id2instrument = Collections.synchronizedMap(new HashMap<String, Instrument>());

  @Override public void opened() throws UnknownInstrumentsException {
    System.out.println("opened: " + hashCode());
    instrumentService.reload();
    for (final Instrument instrument : instrumentService.getInstruments()) {
      id2instrument.put(instrument.id, instrument);
    }
    priceEngine.subscribe(new ArrayList<>(id2instrument.keySet()));
  }

  @Override public void closed(@Nullable final Throwable throwable) {
    System.out.println("closed: " + hashCode() + ", " + throwable);
    if (throwable instanceof Throwable) { // terminate on Throwable
      Exceptions.STD_ERR.uncaughtException(Thread.currentThread(), throwable);
    }
  }

  @Override public Instrument getInstrument(final String instrumentId) {
    return id2instrument.get(instrumentId);
  }

}

package ch.softappeal.yass.tutorial.session.client;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.tutorial.session.contract.Instrument;
import ch.softappeal.yass.tutorial.session.contract.InstrumentService;
import ch.softappeal.yass.tutorial.session.contract.PriceEngine;
import ch.softappeal.yass.tutorial.session.contract.ServerServices;
import ch.softappeal.yass.tutorial.session.contract.UnknownInstrumentsException;
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
    System.out.println("create: " + hashCode() + ", " + ((SocketConnection)connection).socket);
    priceEngine = ServerServices.PriceEngineId.invoker(this).proxy();
    instrumentService = ServerServices.InstrumentServiceId.invoker(this).proxy();
  }

  private final Map<String, Instrument> id2instrument = Collections.synchronizedMap(new HashMap<String, Instrument>());

  @Override public void opened() throws UnknownInstrumentsException {
    System.out.println("opened: " + hashCode());
    for (final Instrument instrument : instrumentService.getInstruments()) {
      id2instrument.put(instrument.id, instrument);
    }
    priceEngine.subscribe(new ArrayList<>(id2instrument.keySet()));
  }

  @Override public void closed(@Nullable final Exception exception) {
    System.out.println("closed: " + hashCode() + ", " + exception);
  }

  @Override public Instrument getInstrument(final String instrumentId) {
    return id2instrument.get(instrumentId);
  }

}

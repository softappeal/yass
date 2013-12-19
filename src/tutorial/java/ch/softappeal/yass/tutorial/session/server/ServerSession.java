package ch.softappeal.yass.tutorial.session.server;

import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.transport.socket.SocketConnection;
import ch.softappeal.yass.tutorial.session.contract.ClientServices;
import ch.softappeal.yass.tutorial.session.contract.DateTime;
import ch.softappeal.yass.tutorial.session.contract.Price;
import ch.softappeal.yass.tutorial.session.contract.PriceListener;
import ch.softappeal.yass.tutorial.session.contract.PriceType;
import ch.softappeal.yass.util.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerSession extends Session implements PriceEngineContext {

  private final PriceListener priceListener;

  public ServerSession(final SessionSetup setup, final Connection connection) {
    super(setup, connection);
    System.out.println("create: " + hashCode() + ", " + ((SocketConnection)connection).socket);
    priceListener = ClientServices.PriceListener.invoker(this).proxy(Logger.CLIENT);
  }

  private final Set<String> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<String>());
  private final AtomicBoolean closed = new AtomicBoolean(false);

  @Override public void opened() throws InterruptedException {
    System.out.println("opened: " + hashCode());
    final Random random = new Random();
    while (!closed.get()) {
      final List<Price> prices = new ArrayList<>();
      for (final String subscribedInstrumentId : subscribedInstrumentIds.toArray(new String[0])) {
        prices.add(new Price(
          subscribedInstrumentId,
          new BigDecimal(random.nextInt(99) + 1),
          PriceType.ASK,
          new DateTime("20130-12-01 14:40:50")
        ));
      }
      priceListener.newPrices(prices);
      TimeUnit.SECONDS.sleep(1L);
    }
  }

  @Override public void closed(@Nullable final Exception exception) {
    closed.set(true);
    System.out.println("closed: " + hashCode() + ", " + exception);
  }

  @Override public Set<String> subscribedInstrumentIds() {
    return subscribedInstrumentIds;
  }

}

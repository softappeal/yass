package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionClient;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceListener;
import ch.softappeal.yass.tutorial.contract.PriceType;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerSession extends Session implements PriceEngineContext {

    private final Set<String> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final PriceListener priceListener;
    private final EchoService echoService;

    public ServerSession(final SessionClient sessionClient) {
        super(sessionClient);
        System.out.println("create: " + hashCode());
        priceListener = invoker(ClientServices.PriceListener).proxy(Logger.CLIENT);
        echoService = invoker(ClientServices.EchoService).proxy();
    }

    @Override public void opened() throws InterruptedException {
        System.out.println("opened: " + hashCode());
        System.out.println(echoService.echo("echo"));
        final Random random = new Random();
        while (!closed.get()) {
            final List<Price> prices = new ArrayList<>();
            for (final String subscribedInstrumentId : subscribedInstrumentIds.toArray(new String[0])) {
                prices.add(new Price(subscribedInstrumentId, random.nextInt(99) + 1, PriceType.ASK));
            }
            if (!prices.isEmpty()) {
                priceListener.newPrices(prices);
            }
            TimeUnit.MILLISECONDS.sleep(1000L);
        }
    }

    @Override public void closed(@Nullable final Throwable throwable) {
        closed.set(true);
        System.out.println("closed: " + hashCode());
        if (throwable != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, throwable);
        }
    }

    @Override public Set<String> subscribedInstrumentIds() {
        return subscribedInstrumentIds;
    }

}

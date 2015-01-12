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

public final class ServerSession extends Session implements PriceEngineContext {

    private final Set<Integer> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<Integer>());

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
        while (!isClosed()) {
            final List<Price> prices = new ArrayList<>();
            for (final int subscribedInstrumentId : subscribedInstrumentIds.toArray(new Integer[0])) {
                if (random.nextBoolean()) {
                    prices.add(new Price(subscribedInstrumentId, random.nextInt(99) + 1, PriceType.values()[random.nextInt(2)]));
                }
            }
            if (!prices.isEmpty()) {
                priceListener.newPrices(prices);
            }
            TimeUnit.MILLISECONDS.sleep(500L);
        }
    }

    @Override public void closed(@Nullable final Throwable throwable) {
        System.out.println("closed: " + hashCode());
        if (throwable != null) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, throwable);
        }
    }

    @Override public Set<Integer> subscribedInstrumentIds() {
        return subscribedInstrumentIds;
    }

}

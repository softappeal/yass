package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.PriceListener;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;
import static ch.softappeal.yass.tutorial.contract.Config.INITIATOR;

public final class AcceptorSession extends SimpleSession {

    private final Set<Integer> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<>());

    @Override protected Server server() {
        final Interceptor interceptor = Interceptor.composite(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            ACCEPTOR.instrumentService.service(InstrumentServiceImpl.INSTANCE, interceptor),
            ACCEPTOR.priceEngine.service(new PriceEngineImpl(InstrumentServiceImpl.INSTRUMENTS, subscribedInstrumentIds), interceptor),
            ACCEPTOR.echoService.service(EchoServiceImpl.INSTANCE, interceptor)
        );
    }

    private final PriceListener priceListener;
    private final EchoService echoService;

    public AcceptorSession(final Connection connection, final Executor dispatchExecutor) {
        super(connection, dispatchExecutor);
        System.out.println("session " + this + " created");
        final Interceptor interceptor = new Logger(this, Logger.Side.CLIENT);
        priceListener = proxy(INITIATOR.priceListener, interceptor);
        echoService = proxy(INITIATOR.echoService, interceptor);
    }

    @Override protected void opened() throws InterruptedException {
        System.out.println("session " + this + " opened");
        System.out.println("echo: " + echoService.echo("hello from acceptor"));
        final Random random = new Random();
        while (!isClosed()) {
            final List<Price> prices = new ArrayList<>();
            for (final int subscribedInstrumentId : subscribedInstrumentIds.toArray(new Integer[0])) {
                if (random.nextBoolean()) {
                    prices.add(new Price(subscribedInstrumentId, random.nextInt(99) + 1, PriceKind.values()[random.nextInt(2)]));
                }
            }
            if (!prices.isEmpty()) {
                priceListener.newPrices(prices);
            }
            TimeUnit.MILLISECONDS.sleep(500L);
        }
    }

    @Override protected void closed(final @Nullable Exception exception) {
        System.out.println("session " + this + " closed: " + exception);
    }

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String id = String.valueOf(ID.getAndIncrement());
    @Override public String toString() {
        return id;
    }

}

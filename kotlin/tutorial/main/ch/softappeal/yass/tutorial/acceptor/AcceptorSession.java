package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.remote.session.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.contract.generic.*;
import ch.softappeal.yass.tutorial.shared.*;
import kotlin.jvm.functions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static ch.softappeal.yass.InterceptorKt.*;
import static ch.softappeal.yass.remote.ServerKt.*;
import static ch.softappeal.yass.remote.session.SessionWatcherKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class AcceptorSession extends SimpleSession {

    private final Set<Integer> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<>());

    @Override
    protected Server server() {
        final Function3<Method, List<?>, Function0<?>, Object> interceptor = compositeInterceptor(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            asyncService(ACCEPTOR.instrumentService, InstrumentServiceImpl.INSTANCE, AsyncLogger.INSTANCE),
            service(ACCEPTOR.priceEngine, new PriceEngineImpl(subscribedInstrumentIds), interceptor),
            service(ACCEPTOR.echoService, EchoServiceImpl.INSTANCE, interceptor),
            service(ACCEPTOR.genericEchoService, GenericEchoServiceImpl.INSTANCE, interceptor)
        );
    }

    private final PriceListener priceListener;
    private final EchoService echoService;

    public AcceptorSession(final Executor dispatchExecutor) {
        super(dispatchExecutor);
        System.out.println("session " + this + " created");
        final Function3<Method, List<?>, Function0<?>, Object> interceptor =
            new Logger(this, Logger.Side.CLIENT);
        priceListener = proxy(INITIATOR.priceListener, interceptor);
        echoService = proxy(INITIATOR.echoService, interceptor);
    }

    @Override
    protected void opened() throws InterruptedException {
        watchSession(getDispatchExecutor(), this, 60L, 2L, 0L, () -> {
            echoService.echo("checkFromAcceptor");
            return null;
        });
        System.out.println("session " + this + " opened start");
        System.out.println("echo: " + echoService.echo("hello from acceptor"));
        try {
            echoService.echo("throwRuntimeException");
        } catch (final SystemException e) {
            System.out.println("echo: " + e.details);
        }
        final Random random = new Random();
        while (!isClosed()) {
            final List<Price> prices = new ArrayList<>();
            for (final int subscribedInstrumentId : subscribedInstrumentIds.toArray(new Integer[0])) {
                if (random.nextBoolean()) {
                    prices.add(
                        new Price(
                            subscribedInstrumentId,
                            random.nextInt(99) + 1,
                            PriceKind.values()[random.nextInt(2)]
                        )
                    );
                }
            }
            if (!prices.isEmpty()) {
                priceListener.newPrices(prices);
            }
            TimeUnit.MILLISECONDS.sleep(500L);
        }
        System.out.println("session " + this + " opened end");
    }

    @Override
    protected void closed(final Exception exception) {
        System.out.println("session " + this + " closed: " + exception);
    }

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String id = String.valueOf(ID.getAndIncrement());

    @Override
    public String toString() {
        return id;
    }

}

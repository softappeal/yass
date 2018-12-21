package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.remote.session.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.contract.generic.*;
import ch.softappeal.yass.tutorial.contract.instrument.*;
import ch.softappeal.yass.tutorial.shared.*;
import kotlin.jvm.functions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static ch.softappeal.yass.DumperKt.*;
import static ch.softappeal.yass.InterceptorKt.*;
import static ch.softappeal.yass.remote.ClientKt.*;
import static ch.softappeal.yass.remote.ServerKt.*;
import static ch.softappeal.yass.remote.session.SessionWatcherKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

public final class InitiatorSession extends SimpleSession {

    @Override
    protected Server server() {
        final Function3<Method, List<?>, Function0<?>, Object> interceptor = compositeInterceptor(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            service(INITIATOR.priceListener, PriceListenerImpl.INSTANCE, interceptor),
            service(INITIATOR.echoService, EchoServiceImpl.INSTANCE, interceptor)
        );
    }

    public final PriceEngine priceEngine;
    public final InstrumentService instrumentService;
    public final EchoService echoService;
    public final GenericEchoService genericEchoService;

    public InitiatorSession(final Executor dispatchExecutor) {
        super(dispatchExecutor);
        System.out.println("session created");
        final Function3<Method, List<?>, Function0<?>, Object> interceptor =
            new Logger(this, Logger.Side.CLIENT);
        priceEngine = proxy(ACCEPTOR.priceEngine, interceptor);
        instrumentService = asyncProxy(ACCEPTOR.instrumentService, AsyncLogger.INSTANCE);
        echoService = proxy(ACCEPTOR.echoService, interceptor);
        genericEchoService = proxy(ACCEPTOR.genericEchoService, interceptor);
    }

    @Override
    protected void opened() {
        watchSession(getDispatchExecutor(), this, 60L, 2L, () -> {
            echoService.echo("checkFromInitiator");
            return null;
        });
        System.out.println("session opened start");
        System.out.println("echo: " + echoService.echo("hello from initiator"));

        final Pair<Boolean, TripleWrapper> result = genericEchoService.echo(new Pair<>(
            true,
            new TripleWrapper(new Triple<>(
                PriceKind.ASK,
                true,
                new Pair<>(
                    "hello",
                    Arrays.asList(new PairBoolBool(true, false), new PairBoolBool(false, true))
                )
            ))
        ));
        System.out.println("genericEcho: " + dump(Logger.DUMPER, result).toString());

        try {
            priceEngine.subscribe(Arrays.asList(123456789, 987654321));
        } catch (final UnknownInstrumentsException e) {
            e.printStackTrace(System.out);
        }
        instrumentService.showOneWay(false, 123);
        promise(instrumentService::getInstruments).thenAcceptAsync(instruments -> {
            try {
                priceEngine
                    .subscribe(instruments.stream().map(instrument -> instrument.id).collect(Collectors.toList()));
            } catch (final UnknownInstrumentsException ignore) {
                // empty
            }
        }, getDispatchExecutor());
        System.out.println("session opened end");
    }

    @Override
    protected void closed(final Exception exception) {
        System.out.println("session closed: " + exception);
    }

}

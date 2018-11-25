package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.session.SimpleSession;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.UnknownInstrumentsException;
import ch.softappeal.yass.tutorial.contract.generic.GenericEchoService;
import ch.softappeal.yass.tutorial.contract.generic.Pair;
import ch.softappeal.yass.tutorial.contract.generic.PairBoolBool;
import ch.softappeal.yass.tutorial.contract.generic.Triple;
import ch.softappeal.yass.tutorial.contract.generic.TripleWrapper;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.shared.AsyncLogger;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static ch.softappeal.yass.DumperKt.dump;
import static ch.softappeal.yass.InterceptorKt.compositeInterceptor;
import static ch.softappeal.yass.remote.ClientKt.promise;
import static ch.softappeal.yass.remote.ServerKt.service;
import static ch.softappeal.yass.remote.session.SessionWatcherKt.watchSession;
import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;
import static ch.softappeal.yass.tutorial.contract.Config.INITIATOR;

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
        watchSession(getDispatchExecutor(), this, 60L, 2L, 0L, () -> {
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

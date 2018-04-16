package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.session.Connection;
import ch.softappeal.yass.remote.session.SessionWatcher;
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
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.LoggerAsync;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;
import static ch.softappeal.yass.tutorial.contract.Config.INITIATOR;

public final class InitiatorSession extends SimpleSession {

    @Override protected Server server() {
        final var interceptor = Interceptor.composite(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            INITIATOR.priceListener.service(PriceListenerImpl.INSTANCE, interceptor),
            INITIATOR.echoService.service(EchoServiceImpl.INSTANCE, interceptor)
        );
    }

    public final PriceEngine priceEngine;
    public final InstrumentService instrumentServiceAsync;
    public final EchoService echoService;
    public final GenericEchoService genericEchoService;

    public InitiatorSession(final Connection connection, final Executor dispatchExecutor) {
        super(connection, dispatchExecutor);
        System.out.println("session created");
        final var interceptor = new Logger(this, Logger.Side.CLIENT);
        priceEngine = proxy(ACCEPTOR.priceEngine, interceptor);
        instrumentServiceAsync = proxyAsync(ACCEPTOR.instrumentService, LoggerAsync.INSTANCE);
        echoService = proxy(ACCEPTOR.echoService, interceptor);
        genericEchoService = proxy(ACCEPTOR.genericEchoService, interceptor);
    }

    @Override protected void opened() {
        SessionWatcher.watchSession(dispatchExecutor, this, 60L, 2L, () -> echoService.echo("checkFromInitiator")); // optional
        System.out.println("session opened start");
        System.out.println("echo: " + echoService.echo("hello from initiator"));

        final var result = genericEchoService.echo(new Pair<>(
            true,
            new TripleWrapper(new Triple<>(
                PriceKind.ASK,
                true,
                new Pair<>(
                    "hello",
                    List.of(new PairBoolBool(true, false), new PairBoolBool(false, true))
                )
            ))
        ));
        System.out.println("genericEcho: " + Logger.dump(result));

        try {
            priceEngine.subscribe(List.of(123456789, 987654321));
        } catch (final UnknownInstrumentsException e) {
            e.printStackTrace(System.out);
        }
        instrumentServiceAsync.showOneWay(false, 123);
        Client.promise(instrumentServiceAsync::getInstruments).thenAcceptAsync(instruments -> {
            try {
                priceEngine.subscribe(instruments.stream().map(instrument -> instrument.id).collect(Collectors.toList()));
            } catch (final UnknownInstrumentsException ignore) {
                // empty
            }
        }, dispatchExecutor);
        System.out.println("session opened end");
    }

    @Override protected void closed(final @Nullable Exception exception) {
        System.out.println("session closed: " + exception);
    }

}

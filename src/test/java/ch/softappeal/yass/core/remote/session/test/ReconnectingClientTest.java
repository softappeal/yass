package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.session.ReconnectingClient;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.transport.socket.SocketExecutor;
import ch.softappeal.yass.transport.socket.SocketTransport;
import ch.softappeal.yass.tutorial.contract.ClientServices;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceEngine;
import ch.softappeal.yass.tutorial.contract.PriceListener;
import ch.softappeal.yass.tutorial.contract.ServerServices;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.server.SocketServer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ReconnectingClientTest {

    @Ignore
    @Test public void test() throws InterruptedException {
        final Server server = new Server(
            Config.METHOD_MAPPER_FACTORY,
            new Service(ClientServices.PriceListener, new PriceListener() {
                @Override public void newPrices(final List<Price> prices) {
                    final StringBuilder s = new StringBuilder();
                    s.append("newPrices - ");
                    for (final Price price : prices) {
                        s.append(' ').append(price.instrumentId).append(":").append(price.value);
                    }
                    System.out.println(s);
                }
            }),
            new Service(ClientServices.EchoService, new EchoServiceImpl())
        );
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        try {
            new ReconnectingClient(executor, 10) {
                @Override protected void connect(final SessionFactory sessionFactory) {
                    SocketTransport.connect(
                        new TransportSetup(server, executor, Config.PACKET_SERIALIZER, sessionFactory),
                        new SocketExecutor(executor, Exceptions.STD_ERR),
                        Config.PATH_SERIALIZER, SocketServer.PATH,
                        SocketServer.ADDRESS
                    );
                }
                @Override protected void connectFailed(final Exception e) {
                    System.out.println("connectFailed: " + e.getMessage());
                }
                @Override protected void connected(final Session session) throws Exception {
                    System.out.println("connected");
                    final PriceEngine priceEngine = session.invoker(ServerServices.PriceEngine).proxy();
                    final InstrumentService instrumentService = session.invoker(ServerServices.InstrumentService).proxy();
                    priceEngine.subscribe(instrumentService.getInstruments().stream().map(instrument -> instrument.id).collect(Collectors.toList()));
                }
                @Override protected void disconnected(@Nullable final Throwable throwable) {
                    System.out.println("disconnected: " + ((throwable == null) ? "<null>" : throwable.getClass()));
                }
            };
            TimeUnit.SECONDS.sleep(60L);
        } finally {
            executor.shutdownNow();
        }
    }

}

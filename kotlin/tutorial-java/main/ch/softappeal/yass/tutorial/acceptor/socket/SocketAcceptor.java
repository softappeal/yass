package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.transport.AcceptorSetup;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.Kt.getStdErr;
import static ch.softappeal.yass.Kt.namedThreadFactory;
import static ch.softappeal.yass.transport.socket.Kt.AsyncSocketConnectionFactory;
import static ch.softappeal.yass.transport.socket.Kt.SocketAcceptor;
import static ch.softappeal.yass.transport.socket.Kt.socketBinder;

public final class SocketAcceptor {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        SocketAcceptor(
            new AcceptorSetup(Config.PACKET_SERIALIZER, () -> new AcceptorSession(executor)),
            executor,
            AsyncSocketConnectionFactory(executor, 1_000)
        ).start(executor, socketBinder(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}

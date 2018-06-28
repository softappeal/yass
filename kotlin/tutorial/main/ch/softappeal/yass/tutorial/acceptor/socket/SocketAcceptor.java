package ch.softappeal.yass.tutorial.acceptor.socket;

import ch.softappeal.yass.transport.AcceptorSetup;
import ch.softappeal.yass.tutorial.acceptor.AcceptorSession;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.socket.SocketSetup;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.ThreadFactoryKt.getStdErr;
import static ch.softappeal.yass.ThreadFactoryKt.namedThreadFactory;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.asyncSocketConnectionFactory;
import static ch.softappeal.yass.transport.socket.SessionSocketTransportKt.socketAcceptor;
import static ch.softappeal.yass.transport.socket.SocketKt.socketBinder;

public final class SocketAcceptor {

    public static void main(final String... args) {
        final Executor executor = Executors.newCachedThreadPool(namedThreadFactory("executor", getStdErr()));
        socketAcceptor(
            new AcceptorSetup(Config.PACKET_SERIALIZER, () -> new AcceptorSession(executor)),
            executor,
            asyncSocketConnectionFactory(executor, 1_000)
        ).start(executor, socketBinder(SocketSetup.ADDRESS));
        System.out.println("started");
    }

}

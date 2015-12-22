package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.session.test.SessionTest;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class TransportTest extends SessionTest {

    protected static TransportSetup transportSetup(final boolean invoke, final boolean createException, final Executor dispatchExecutor) {
        return TransportSetup.ofPacketSerializer(
            JavaSerializer.INSTANCE,
            sessionFactory(invoke, createException, dispatchExecutor)
        );
    }

    protected static TransportSetup transportSetup(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples) {
        return TransportSetup.ofPacketSerializer(
            JavaSerializer.INSTANCE,
            sessionFactory(dispatchExecutor, latch, samples)
        );
    }

    protected static TransportSetup transportSetup(final Executor dispatchExecutor) {
        return TransportSetup.ofPacketSerializer(
            JavaSerializer.INSTANCE,
            sessionFactory(dispatchExecutor, null, 0)
        );
    }

}

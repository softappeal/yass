package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.session.test.SessionTest;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Nullable;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class TransportTest extends SessionTest {

    private static final Serializer CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        Collections.singletonList(BaseTypeHandlers.INTEGER),
        Collections.emptyList(),
        Collections.singletonList(DivisionByZeroException.class),
        Collections.emptyList()
    );

    protected static TransportSetup transportSetup(final boolean invoke, final boolean createException, final Executor dispatchExecutor) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            sessionFactory(invoke, createException, dispatchExecutor)
        );
    }

    protected static TransportSetup transportSetup(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            sessionFactory(dispatchExecutor, latch, samples)
        );
    }

    protected static TransportSetup transportSetup(final Executor dispatchExecutor) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            sessionFactory(dispatchExecutor, null, 0)
        );
    }

}

package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.session.test.SessionTest;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class TransportTest extends SessionTest {

    protected static final Serializer CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(BaseTypeHandlers.INTEGER, BaseTypeHandlers.BYTE_ARRAY),
        Collections.emptyList(),
        Collections.singletonList(DivisionByZeroException.class),
        Collections.emptyList()
    );

    protected static TransportSetup invokeTransportSetup(final boolean invoke, final boolean createException, final Executor dispatchExecutor) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            invokeSessionFactory(invoke, createException, dispatchExecutor)
        );
    }

    protected static TransportSetup performanceTransportSetup(final Executor dispatchExecutor, final @Nullable CountDownLatch latch, final int samples, final int bytes) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            performanceSessionFactory(dispatchExecutor, latch, samples, bytes)
        );
    }

    protected static TransportSetup performanceTransportSetup(final Executor dispatchExecutor) {
        return TransportSetup.ofContractSerializer(
            CONTRACT_SERIALIZER,
            performanceSessionFactory(dispatchExecutor)
        );
    }

}

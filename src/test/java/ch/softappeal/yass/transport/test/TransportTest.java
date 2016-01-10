package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.core.remote.session.test.SessionTest;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.util.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class TransportTest extends SessionTest {

    private static final Serializer CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        Arrays.<BaseTypeHandler<?>>asList(BaseTypeHandlers.INTEGER, BaseTypeHandlers.BYTE_ARRAY),
        Collections.<Class<?>>emptyList(),
        Collections.<Class<?>>singletonList(DivisionByZeroException.class),
        Collections.<Class<?>>emptyList()
    );

    protected static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(CONTRACT_SERIALIZER);

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

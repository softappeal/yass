package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.AbstractJsFastSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleJsFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedJsFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.Arrays;
import java.util.Collection;

public final class Config {

    private static final Collection<Class<?>> ENUMERATIONS = Arrays.asList(
        PriceKind.class
    );

    private static final Collection<Class<?>> CONCRETE_CLASSES = Arrays.asList(
        Price.class,
        Stock.class,
        Bond.class,
        SystemException.class,
        UnknownInstrumentsException.class
    );

    private static final Collection<Class<?>> REFERENCEABLE_CONCRETE_CLASSES = Arrays.asList(
        Node.class
    );

    /**
     * Shows how to use {@link SimpleJsFastSerializer}.
     */
    private static final AbstractJsFastSerializer SIMPLE_JS_CONTRACT_SERIALIZER = new SimpleJsFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            Expiration.TYPE_HANDLER,
            BaseTypeHandlers.INTEGER
        ),
        ENUMERATIONS,
        CONCRETE_CLASSES
    );

    /**
     * Shows how to use {@link TaggedJsFastSerializer}.
     */
    private static final AbstractJsFastSerializer TAGGED_JS_CONTRACT_SERIALIZER = new TaggedJsFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            new TypeDesc(AbstractJsFastSerializer.FIRST_ID, Expiration.TYPE_HANDLER),
            new TypeDesc(AbstractJsFastSerializer.FIRST_ID + 1, BaseTypeHandlers.INTEGER)
        ),
        ENUMERATIONS,
        CONCRETE_CLASSES
    );

    public static final AbstractJsFastSerializer JS_CONTRACT_SERIALIZER = SIMPLE_JS_CONTRACT_SERIALIZER;

    /**
     * Shows how to use {@link SimpleFastSerializer}.
     */
    private static final AbstractFastSerializer SIMPLE_CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            BaseTypeHandlers.BOOLEAN,
            BaseTypeHandlers.INTEGER,
            BaseTypeHandlers.DOUBLE,
            BaseTypeHandlers.STRING,
            Expiration.TYPE_HANDLER
        ),
        ENUMERATIONS,
        CONCRETE_CLASSES,
        REFERENCEABLE_CONCRETE_CLASSES
    );

    /**
     * Shows how to use {@link TaggedFastSerializer}.
     */
    private static final AbstractFastSerializer TAGGED_CONTRACT_SERIALIZER = new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            new TypeDesc(TypeDesc.FIRST_ID, BaseTypeHandlers.BOOLEAN),
            new TypeDesc(TypeDesc.FIRST_ID + 1, BaseTypeHandlers.INTEGER),
            new TypeDesc(TypeDesc.FIRST_ID + 2, BaseTypeHandlers.DOUBLE),
            new TypeDesc(TypeDesc.FIRST_ID + 3, BaseTypeHandlers.STRING),
            new TypeDesc(TypeDesc.FIRST_ID + 4, Expiration.TYPE_HANDLER)
        ),
        ENUMERATIONS,
        CONCRETE_CLASSES,
        REFERENCEABLE_CONCRETE_CLASSES
    );

    public static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(JS_CONTRACT_SERIALIZER);

    public static final Serializer PACKET_SERIALIZER = new PacketSerializer(MESSAGE_SERIALIZER);

    /**
     * Shows how to use {@link SimpleMethodMapper}.
     */
    private static final MethodMapper.Factory SIMPLE_METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    /**
     * Shows how to use {@link TaggedMethodMapper}.
     */
    private static final MethodMapper.Factory TAGGED_METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

    public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SIMPLE_METHOD_MAPPER_FACTORY;

}

package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.AbstractJsFastSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleJsFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedJsFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.Arrays;
import java.util.Collection;

public final class Config {

    private static final Collection<Class<?>> ENUMERATIONS = Arrays.<Class<?>>asList(
        PriceKind.class
    );

    private static final Collection<Class<?>> CONCRETE_CLASSES = Arrays.asList(
        Price.class,
        Stock.class,
        Bond.class,
        SystemException.class,
        UnknownInstrumentsException.class
    );

    private static final Collection<Class<?>> REFERENCEABLE_CONCRETE_CLASSES = Arrays.<Class<?>>asList(
        Node.class
    );

    /**
     * Shows how to use {@link SimpleJsFastSerializer}.
     */
    private static final AbstractJsFastSerializer SIMPLE_JS_SERIALIZER = new SimpleJsFastSerializer(
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
    private static final AbstractJsFastSerializer TAGGED_JS_SERIALIZER = new TaggedJsFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            new TypeDesc(AbstractJsFastSerializer.FIRST_ID, Expiration.TYPE_HANDLER),
            new TypeDesc(AbstractJsFastSerializer.FIRST_ID + 1, BaseTypeHandlers.INTEGER)
        ),
        ENUMERATIONS,
        CONCRETE_CLASSES
    );

    /**
     * Shows how to use {@link SimpleFastSerializer}.
     */
    private static final Serializer SIMPLE_SERIALIZER = new SimpleFastSerializer(
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
    private static final Serializer TAGGED_SERIALIZER = new TaggedFastSerializer(
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

    public static final AbstractJsFastSerializer SERIALIZER = SIMPLE_JS_SERIALIZER;

    /**
     * Shows how to use {@link SimpleMethodMapper}.
     */
    private static final MethodMapper.Factory SIMPLE_METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    /**
     * Shows how to use {@link TaggedMethodMapper}.
     */
    private static final MethodMapper.Factory TAGGED_METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

    private static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SIMPLE_METHOD_MAPPER_FACTORY;

    private abstract static class Role extends Services {
        Role() {
            super(METHOD_MAPPER_FACTORY);
        }
    }

    public static final class Initiator extends Role {
        public final ContractId<PriceListener> priceListener = create(PriceListener.class, 0);
        public final ContractId<EchoService> echoService = create(EchoService.class, 1);
    }

    public static final class Acceptor extends Role {
        public final ContractId<PriceEngine> priceEngine = create(PriceEngine.class, 0);
        public final ContractId<InstrumentService> instrumentService = create(InstrumentService.class, 1);
        public final ContractId<EchoService> echoService = create(EchoService.class, 2);
    }

    public static final Initiator INITIATOR = new Initiator();
    public static final Acceptor ACCEPTOR = new Acceptor();

}

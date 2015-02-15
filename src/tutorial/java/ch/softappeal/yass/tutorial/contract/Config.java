package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.JsFastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.transport.StringPathSerializer;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.stock.JsDouble;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.Arrays;

public final class Config {

    /**
     * @see TaggedFastSerializer
     * @see SimpleFastSerializer
     */
    public static final JsFastSerializer CONTRACT_SERIALIZER = new JsFastSerializer(
        FastReflector.FACTORY,
        Arrays.asList(
            Expiration.TYPE_HANDLER,
            JsDouble.TYPE_HANDLER
        ),
        Arrays.<Class<?>>asList(
            PriceType.class
        ),
        Arrays.<Class<?>>asList(
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class
        )
    );

    public static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(CONTRACT_SERIALIZER);

    public static final Serializer PACKET_SERIALIZER = new PacketSerializer(MESSAGE_SERIALIZER);

    /**
     * @see TaggedMethodMapper
     */
    public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = SimpleMethodMapper.FACTORY;

    public static final Serializer PATH_SERIALIZER = StringPathSerializer.INSTANCE;

}

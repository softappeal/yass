package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.JsFastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.Stock;

import java.util.Arrays;

public final class Config {

  /**
   * @see TaggedFastSerializer
   * @see SimpleFastSerializer
   */
  public static final JsFastSerializer CONTRACT_SERIALIZER = new JsFastSerializer(
    FastReflector.FACTORY,
    Arrays.<Class<?>>asList(PriceType.class),
    Arrays.<Class<?>>asList(Price.class, Trade.class, UnknownInstrumentsException.class),
    Arrays.<Class<?>>asList(Stock.class, Bond.class)
  );

  /**
   * Shows how to configure a {@link SimpleFastSerializer}.
   */
  private static final AbstractFastSerializer CONTRACT_SERIALIZER_2 = new SimpleFastSerializer(
    FastReflector.FACTORY,
    Arrays.<BaseTypeHandler<?>>asList(
      BaseTypeHandlers.BOOLEAN,
      BaseTypeHandlers.INTEGER,
      BaseTypeHandlers.STRING,
      DateTime.TYPE_HANDLER
    ),
    Arrays.<Class<?>>asList(PriceType.class),
    Arrays.<Class<?>>asList(Price.class, Trade.class, UnknownInstrumentsException.class),
    Arrays.<Class<?>>asList(Stock.class, Bond.class)
  );

  public static final Serializer PACKET_SERIALIZER = new PacketSerializer(new MessageSerializer(CONTRACT_SERIALIZER));

  /**
   * @see SimpleMethodMapper
   */
  public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = TaggedMethodMapper.FACTORY;

}

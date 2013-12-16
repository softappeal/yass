package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.convert.StringTypeConverter;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeConverterId;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.PacketSerializer;
import ch.softappeal.yass.util.Dumper;

import java.math.BigDecimal;
import java.util.Arrays;

public final class Config {

  public static final AbstractFastSerializer CONTRACT_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY,
    Arrays.<TypeConverterId>asList(
      new TypeConverterId(StringTypeConverter.BIG_DECIMAL, 0),
      new TypeConverterId(DateTime.TYPE_CONVERTER, 50)
    ),
    Arrays.<Class<?>>asList(PriceType.class),
    Arrays.<Class<?>>asList(Price.class, UnknownInstrumentsException.class),
    Arrays.<Class<?>>asList(Instrument.class)
  );

  public static final Serializer PACKET_SERIALIZER = new PacketSerializer(new MessageSerializer(CONTRACT_SERIALIZER));

  public static final Dumper DUMPER = new Dumper(BigDecimal.class, DateTime.class);

  public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = MethodMapper.TAG_FACTORY;

}

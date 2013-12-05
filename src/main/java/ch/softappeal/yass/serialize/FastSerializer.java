package ch.softappeal.yass.serialize;

import ch.softappeal.yass.serialize.convert.TypeConverter;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.reflect.Reflector;

import java.util.List;

/**
 * This class guarantees compatibility with java 1.6 backport.
 * Use {@link SimpleFastSerializer} instead.
 */
@Deprecated
public final class FastSerializer implements Serializer {

  private final Serializer serializer;

  public FastSerializer(
    final Reflector.Factory reflectorFactory,
    final List<TypeConverter> typeConverters,
    final List<Class<?>> enumerations,
    final List<Class<?>> concreteClasses,
    final List<Class<?>> referenceableConcreteClasses
  ) {
    serializer = new SimpleFastSerializer(
      reflectorFactory,
      typeConverters,
      enumerations,
      concreteClasses,
      referenceableConcreteClasses
    );
  }

  @Override public Object read(final Reader reader) throws Exception {
    return serializer.read(reader);
  }

  @Override public void write(final Object value, final Writer writer) throws Exception {
    serializer.write(value, writer);
  }

}

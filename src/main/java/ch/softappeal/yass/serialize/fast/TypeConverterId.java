package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.util.Check;

public final class TypeConverterId {

  public final TypeConverter<?, ?> typeConverter;
  public final int id;

  public TypeConverterId(final TypeConverter<?, ?> typeConverter, final int id) {
    this.typeConverter = Check.notNull(typeConverter);
    this.id = id;
  }

}

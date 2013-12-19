package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.util.Nullable;

public final class ConverterTypeHandler extends TypeHandler {

  public final BaseTypeHandler<?> serializableTypeHandler;
  private final TypeConverter<Object, Object> typeConverter;

  @SuppressWarnings("unchecked")
  public ConverterTypeHandler(final TypeConverter<?, ?> typeConverter, final int id, final AbstractFastSerializer serializer) {
    super(typeConverter.type, id);
    @Nullable final TypeHandler typeHandler = serializer.class2typeHandler.get(typeConverter.serializableType);
    if (typeHandler == null) {
      throw new IllegalArgumentException(
        "type converter '" + typeConverter.type.getCanonicalName() +
        "': no serializable type '" + typeConverter.serializableType.getCanonicalName() + '\''
      );
    } else if (!(typeHandler instanceof  BaseTypeHandler)) {
      throw new IllegalArgumentException(
        "type converter '" + typeConverter.type.getCanonicalName() +
        "': illegal serializable type '" + typeConverter.serializableType.getCanonicalName() + '\''
      );
    }
    this.serializableTypeHandler = (BaseTypeHandler)typeHandler;
    this.typeConverter = (TypeConverter)typeConverter;
  }

  @Override Object readNoId(Input input) throws Exception {
    return typeConverter.from(serializableTypeHandler.readNoId(input));
  }

  @Override void writeNoId(Object value, Output output) throws Exception {
    serializableTypeHandler.writeNoId(typeConverter.to(value), output);
  }

}

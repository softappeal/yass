package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Check;

/**
 * Converts a type to a serializable type.
 * @param <T> the type to be converted
 * @param <S> the serializable type
 */
public abstract class TypeConverter<T, S> {

  public final Class<T> type;
  public final Class<S> serializableType;

  protected TypeConverter(final Class<T> type, final Class<S> serializableType) {
    this.type = Check.notNull(type);
    this.serializableType = Check.notNull(serializableType);
  }

  public abstract S to(T value) throws Exception;

  public abstract T from(S value) throws Exception;

}

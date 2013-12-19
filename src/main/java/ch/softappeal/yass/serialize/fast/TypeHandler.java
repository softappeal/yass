package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.util.Check;

public abstract class TypeHandler {

  public final Class<?> type;
  public final int id;

  TypeHandler(final Class<?> type, final int id) {
    this.type = Check.notNull(type);
    if (id < 0) {
      // note: due to call to writeVarInt below
      throw new IllegalArgumentException("id " + id + " for type '" + type.getCanonicalName() + "' must be >= 0");
    }
    this.id = id;
  }

  abstract Object readNoId(Input input) throws Exception;

  abstract void writeNoId(Object value, Output output) throws Exception;

  void writeWithId(final Object value, final Output output) throws Exception {
    output.writer.writeVarInt(id);
    writeNoId(value, output);
  }

}

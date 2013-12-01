package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.List;
import java.util.Map;

final class Output {

  final Writer writer;
  private final Map<Class<?>, TypeHandler> class2typeHandler;
  Map<Object, Integer> object2reference = null;

  Output(final Writer writer, final Map<Class<?>, TypeHandler> class2typeHandler) {
    this.writer = Check.notNull(writer);
    this.class2typeHandler = Check.notNull(class2typeHandler);
  }

  void writeWithId(@Nullable final Object value) throws Exception {
    if (value == null) {
      TypeHandlers.NULL.writeWithId(null, this);
    } else if (value instanceof List) {
      TypeHandlers.LIST.writeWithId(value, this);
    } else {
      @Nullable final TypeHandler typeHandler = class2typeHandler.get(value.getClass());
      if (typeHandler == null) {
        throw new IllegalArgumentException("missing type '" + value.getClass().getCanonicalName() + '\'');
      }
      typeHandler.writeWithId(value, this);
    }
  }

}

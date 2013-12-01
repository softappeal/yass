package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.util.List;

abstract class Input {

  final Reader reader;
  List<Object> referenceableObjects = null;

  Input(final Reader reader) {
    this.reader = Check.notNull(reader);
  }

  abstract TypeHandler typeHandler(int id);

  @Nullable final Object readWithId() throws Exception {
    return typeHandler(reader.readVarInt()).readNoId(this);
  }

}

package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class ClassTypeHandler extends TypeHandler {

  private final Reflector reflector;
  public final boolean referenceable;
  private final FieldHandler[] fieldHandlers;

  public final FieldHandler[] fieldHandlers() {
    return fieldHandlers.clone();
  }

  protected ClassTypeHandler(
    final Class<?> type, final int id,
    final Reflector reflector, final boolean referenceable,
    final Collection<FieldHandler> fieldHandlers
  ) {
    super(type, id);
    this.reflector = Check.notNull(reflector);
    this.referenceable = referenceable;
    this.fieldHandlers = fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()]);
    Arrays.sort(this.fieldHandlers, new Comparator<FieldHandler>() { // guarantees field order
      @Override public int compare(final FieldHandler fieldHandler1, final FieldHandler fieldHandler2) {
        return Integer.valueOf(fieldHandler1.id).compareTo(fieldHandler2.id);
      }
    });
  }

  protected abstract FieldHandler fieldHandler(int id);

  @Override final Object readNoId(final Input input) throws Exception {
    final Object object = reflector.newInstance();
    if (referenceable) {
      if (input.referenceableObjects == null) {
        input.referenceableObjects = new ArrayList<>(16);
      }
      input.referenceableObjects.add(object);
    }
    while (true) {
      final int id = input.reader.readVarInt();
      if (id == FieldHandler.END_OF_FIELDS) {
        return object;
      }
      fieldHandler(id).readNoId(object, input);
    }
  }

  @Override final void writeWithId(final Object value, final Output output) throws Exception {
    if (referenceable) {
      if (output.object2reference == null) {
        output.object2reference = new IdentityHashMap<>(16);
      }
      final Map<Object, Integer> object2reference = output.object2reference;
      final Integer reference = object2reference.get(value);
      if (reference != null) {
        TypeHandlers.REFERENCE.writeWithId(reference, output);
        return;
      }
      object2reference.put(value, object2reference.size());
    }
    super.writeWithId(value, output);
  }

  @Override final void writeNoId(final Object value, final Output output) throws Exception {
    for (final FieldHandler fieldHandler : fieldHandlers) {
      fieldHandler.writeWithId(value, output);
    }
    output.writer.writeVarInt(FieldHandler.END_OF_FIELDS);
  }

}

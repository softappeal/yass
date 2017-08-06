package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.util.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ClassTypeHandler extends TypeHandler {

    public static final class FieldDesc {
        public final int id;
        public final FieldHandler handler;
        FieldDesc(final int id, final FieldHandler handler) {
            this.id = id;
            this.handler = handler;
        }
    }

    private final Supplier<Object> instantiator;
    public final boolean referenceable;
    private final Map<Integer, FieldHandler> id2fieldHandler;

    private final FieldDesc[] fieldDescs;
    public FieldDesc[] fieldDescs() {
        return fieldDescs.clone();
    }

    ClassTypeHandler(final Class<?> type, final Function<Class<?>, Supplier<Object>> instantiators, final boolean referenceable, final Map<Integer, FieldHandler> id2fieldHandler) {
        super(type);
        instantiator = Objects.requireNonNull(instantiators.apply(type));
        this.referenceable = referenceable;
        fieldDescs = new FieldDesc[id2fieldHandler.size()];
        int fd = 0;
        for (final Map.Entry<Integer, FieldHandler> entry : id2fieldHandler.entrySet()) {
            final FieldDesc fieldDesc = new FieldDesc(entry.getKey(), entry.getValue());
            if (fieldDesc.id < FieldHandler.FIRST_ID) {
                throw new IllegalArgumentException("id " + fieldDesc.id + " for field '" + fieldDesc.handler.field + "' must be >= " + FieldHandler.FIRST_ID);
            }
            fieldDescs[fd++] = fieldDesc;
        }
        this.id2fieldHandler = new HashMap<>(id2fieldHandler);
        Arrays.sort(fieldDescs, Comparator.comparingInt(fieldDesc -> fieldDesc.id));
    }

    void fixupFields(final Map<Class<?>, TypeDesc> class2typeDesc) {
        id2fieldHandler.values().forEach(fieldHandler -> fieldHandler.fixup(class2typeDesc));
    }

    /**
     * @see FieldHandler#write(int, Object, Output)
     */
    @Override Object read(final Input input) throws Exception {
        final Object object = instantiator.get();
        if (referenceable) {
            if (input.referenceableObjects == null) {
                input.referenceableObjects = new ArrayList<>(16);
            }
            input.referenceableObjects.add(object);
        }
        while (true) {
            final int id = input.reader.readVarInt();
            if (id == FieldHandler.END_ID) {
                return object;
            }
            id2fieldHandler.get(id).read(object, input);
        }
    }

    @Override void write(final int id, final Object value, final Output output) throws Exception {
        if (referenceable) {
            if (output.object2reference == null) {
                output.object2reference = new IdentityHashMap<>(16);
            }
            final Map<Object, Integer> object2reference = output.object2reference;
            final @Nullable Integer reference = object2reference.get(value);
            if (reference != null) {
                TypeDesc.REFERENCE.write(reference, output);
                return;
            }
            object2reference.put(value, object2reference.size());
        }
        super.write(id, value, output);
    }

    @Override void write(final Object value, final Output output) throws Exception {
        for (final FieldDesc fieldDesc : fieldDescs) {
            fieldDesc.handler.write(fieldDesc.id, value, output);
        }
        output.writer.writeVarInt(FieldHandler.END_ID);
    }

}

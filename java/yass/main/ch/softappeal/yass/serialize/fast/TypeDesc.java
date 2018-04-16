package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeDesc {

    public final int id;
    public final TypeHandler handler;

    /**
     * @param id see {@link TypeHandler#write(int, Object, Output)}
     */
    public TypeDesc(final int id, final TypeHandler handler) {
        this.handler = Objects.requireNonNull(handler);
        if (id < 0) {
            throw new IllegalArgumentException("id " + id + " for type '" + handler.type.getCanonicalName() + "' must be >= 0");
        }
        this.id = id;
    }

    void write(final Object value, final Output output) throws Exception {
        handler.write(id, value, output);
    }

    private static final class VoidType {
        // empty
    }

    public static final TypeDesc NULL = new TypeDesc(0, new TypeHandler(VoidType.class) {
        @Override @Nullable Object read(final Input input) {
            return null;
        }
        @Override void write(final @Nullable Object value, final Output output) {
            // empty
        }
    });

    private static final class ReferenceType {
        // empty
    }

    public static final TypeDesc REFERENCE = new TypeDesc(1, new TypeHandler(ReferenceType.class) {
        @Override Object read(final Input input) throws Exception {
            return input.referenceableObjects.get(input.reader.readVarInt());
        }
        @Override void write(final Object value, final Output output) throws Exception {
            output.writer.writeVarInt((Integer)value);
        }
    });

    public static final TypeDesc LIST = new TypeDesc(2, new TypeHandler(List.class) {
        @Override Object read(final Input input) throws Exception {
            var length = input.reader.readVarInt();
            final List<Object> list = new ArrayList<>(Math.min(length, 32)); // note: prevents out-of-memory attack
            while (length-- > 0) {
                list.add(input.read());
            }
            return list;
        }
        @SuppressWarnings("unchecked")
        @Override void write(final Object value, final Output output) throws Exception {
            final var list = (List<Object>)value;
            output.writer.writeVarInt(list.size());
            for (final var e : list) {
                output.write(e);
            }
        }
    });

    public static final int FIRST_ID = 3;

}

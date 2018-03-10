package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.util.Nullable;

import java.util.Objects;

public abstract class TypeHandler {

    public final Class<?> type;

    TypeHandler(final Class<?> type) {
        this.type = Objects.requireNonNull(type);
    }

    abstract @Nullable Object read(Input input) throws Exception;

    abstract void write(@Nullable Object value, Output output) throws Exception;

    /**
     * @see Input#read()
     * @see TypeDesc#TypeDesc(int, TypeHandler)
     */
    void write(final int id, final Object value, final Output output) throws Exception {
        output.writer.writeVarInt(id);
        write(value, output);
    }

}

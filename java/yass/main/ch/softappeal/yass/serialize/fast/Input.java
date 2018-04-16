package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.serialize.Reader;

import java.util.List;
import java.util.Map;

final class Input {

    final Reader reader;
    private final Map<Integer, TypeHandler> id2typeHandler;
    @Nullable List<Object> referenceableObjects;

    Input(final Reader reader, final Map<Integer, TypeHandler> id2typeHandler) {
        this.reader = reader;
        this.id2typeHandler = id2typeHandler;
    }

    /**
     * @see TypeHandler#write(int, Object, Output)
     */
    @Nullable Object read() throws Exception {
        return id2typeHandler.get(reader.readVarInt()).read(this);
    }

}

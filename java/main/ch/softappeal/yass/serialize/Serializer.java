package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Nullable;

/**
 * Writes and reads values.
 */
public interface Serializer {

    @Nullable Object read(Reader reader) throws Exception;

    void write(@Nullable Object value, Writer writer) throws Exception;

}

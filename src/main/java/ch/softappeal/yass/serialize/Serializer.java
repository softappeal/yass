package ch.softappeal.yass.serialize;

/**
 * Writes and reads values.
 */
public interface Serializer {

  Object read(Reader reader) throws Exception;

  void write(Object value, Writer writer) throws Exception;

}

package ch.softappeal.yass.serialize.reflect;

import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Field;

/**
 * Instantiates objects and sets/gets fields.
 */
public interface Reflector {


  interface Accessor {

    @Nullable Object get(Object object) throws Exception;

    void set(Object object, @Nullable Object value) throws Exception;

  }


  Object newInstance() throws Exception;

  Accessor accessor(Field field);


  interface Factory {

    Reflector create(Class<?> type) throws Exception;

  }


}

package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses {@link Tag} as {@link Request#methodId}.
 * Uses {@link OneWay} for marking oneway methods.
 */
public final class TaggedMethodMapper implements MethodMapper {

  private final Map<Integer, Mapping> id2mapping;

  public TaggedMethodMapper(final Class<?> contract) {
    final Method[] methods = contract.getMethods();
    id2mapping = new HashMap<>(methods.length);
    for (final Method method : methods) {
      final int id = Check.hasTag(method);
      if (id2mapping.put(id, new Mapping(method, id, method.getAnnotation(OneWay.class) != null)) != null) {
        throw new IllegalArgumentException("tag '" + id + "' of method '" + method + "' already used");
      }
    }
  }

  @Override public Mapping mapId(final Object id) {
    return id2mapping.get(id);
  }

  @Override public Mapping mapMethod(final Method method) {
    return id2mapping.get(method.getAnnotation(Tag.class).value());
  }

  public void print(final PrintWriter printer) {
    final List<Mapping> mappings = new ArrayList<>(id2mapping.values());
    Collections.sort(mappings, new Comparator<Mapping>() {
      @Override public int compare(final Mapping mapping1, final Mapping mapping2) {
        return ((Integer)mapping1.id).compareTo((Integer)mapping2.id);
      }
    });
    for (final Mapping mapping : mappings) {
      printer.println(mapping.id + ": " + mapping.method);
    }
  }

  public static final Factory FACTORY = new Factory() {
    @Override public MethodMapper create(final Class<?> contract) {
      return new TaggedMethodMapper(contract);
    }
  };

}

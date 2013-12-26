package ch.softappeal.yass.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class Dumper {

  private static final Set<Class<?>> PRIMITIVE_WRAPPER_CLASSES = new HashSet<>(
    Arrays.<Class<?>>asList(
      Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
    )
  );

  private final Set<Class<?>> concreteValueClasses;

  /**
   * @param concreteValueClasses should implement {@link Object#toString()}
   */
  public Dumper(final Class<?>... concreteValueClasses) {
    this.concreteValueClasses = new HashSet<>(Arrays.asList(concreteValueClasses));
    this.concreteValueClasses.addAll(PRIMITIVE_WRAPPER_CLASSES);
  }

  @SuppressWarnings("WeakerAccess")
  public StringBuilder append(final StringBuilder s, @Nullable final Object value) {
    try {
      new Dump(s).dump(value);
      return s;
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  public String toString(@Nullable final Object value) {
    return append(new StringBuilder(256), value).toString();
  }

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private final class Dump {

    private final StringBuilder out;

    private Dump(final StringBuilder out) {
      this.out = Check.notNull(out);
    }

    private int tabs = 0;

    private void appendTabs() {
      for (int t = tabs; t > 0; t--) {
        out.append("  ");
      }
    }

    private void appendLine() {
      out.append(LINE_SEPARATOR);
    }

    private void inc(final String s) {
      out.append(s);
      appendLine();
      tabs++;
    }

    private void dec(final String s) {
      tabs--;
      appendTabs();
      out.append(s);
    }

    private void dumpArray(final Object array) throws Exception {
      final int length = Array.getLength(array);
      inc("[");
      for (int i = 0; i < length; i++) {
        appendTabs();
        dump(Array.get(array, i));
        appendLine();
      }
      dec("]");
    }

    private void dumpCollection(final Collection<?> collection) throws Exception {
      inc("[");
      for (final Object e : collection) {
        appendTabs();
        dump(e);
        appendLine();
      }
      dec("]");
    }

    private void dumpMap(final Map<?, ?> map) throws Exception {
      inc("{");
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        appendTabs();
        dump(entry.getKey());
        out.append(" -> ");
        dump(entry.getValue());
        appendLine();
      }
      dec("}");
    }

    private void dumpClassFields(Class<?> type, final Object object) throws Exception {
      while ((type != Object.class) && (type != Throwable.class)) {
        for (final Field field : type.getDeclaredFields()) {
          final int modifiers = field.getModifiers();
          if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
            field.setAccessible(true);
            @Nullable final Object value = field.get(object);
            if (value != null) {
              appendTabs();
              out.append(field.getName()).append(" = ");
              dump(value);
              appendLine();
            }
          }
        }
        //noinspection AssignmentToMethodParameter
        type = type.getSuperclass();
      }
    }

    private final Map<Object, Integer> alreadyDumpedObjects = new IdentityHashMap<>(16);

    private void dumpClass(final Class<?> type, final Object object) throws Exception {
      final Integer reference = alreadyDumpedObjects.get(object);
      if (reference != null) {
        out.append('#').append(reference);
      } else {
        final int index = alreadyDumpedObjects.size();
        alreadyDumpedObjects.put(object, index);
        final Class<?> toStringClass = type.getMethod("toString").getDeclaringClass();
        if ((toStringClass == Object.class) || (toStringClass == Throwable.class)) {
          inc(type.getSimpleName() + '(');
          dumpClassFields(type, object);
          dec(")");
        } else {
          out.append(object);
        }
        out.append(" #").append(index);
      }
    }

    void dump(@Nullable final Object value) throws Exception {
      if (value == null) {
        out.append("null");
        return;
      }
      final Class<?> type = value.getClass();
      if (type == Character.class) {
        final char c = (Character)value;
        if ((c >= (char)32) && (c <= (char)126)) {
          out.append('\'').append(c).append('\'');
        } else {
          out.append("(char)").append((int)c);
        }
      } else if (type.isEnum() || concreteValueClasses.contains(type)) {
        out.append(value);
      } else if (value instanceof CharSequence) {
        out.append('"').append(value).append('"');
      } else if (type.isArray()) {
        dumpArray(value);
      } else if (value instanceof Collection) {
        dumpCollection((Collection<?>)value);
      } else if (value instanceof Map) {
        dumpMap((Map<?, ?>)value);
      } else {
        dumpClass(type, value);
      }
    }
  }

}

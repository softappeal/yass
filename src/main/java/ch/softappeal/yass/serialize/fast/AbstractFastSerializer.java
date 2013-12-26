package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.serialize.Writer;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This fast and compact serializer (using numbers) supports the following types:
 * <ul>
 * <li>null</li>
 * <li>all primitive types (including array and wrapper thereof)</li>
 * <li>{@link String}</li>
 * <li>{@link TypeConverter}</li>
 * <li>enumeration types (an enumeration constant is serialized with its ordinal number)</li>
 * <li>{@link List} (deserialize creates an {@link ArrayList})</li>
 * <li>class hierarchies with all non-static and non-transient fields</li>
 * <li>exceptions (but without fields of {@link Throwable}; therefore, you should implement {@link Throwable#getMessage()})</li>
 * <li>graphs with cycles</li>
 * </ul>
 */
public abstract class AbstractFastSerializer implements Serializer {

  final Map<Class<?>, TypeHandler> class2typeHandler = new HashMap<>(16);

  protected final void addTypeHandler(final TypeHandler typeHandler) {
    if (class2typeHandler.put(typeHandler.type, typeHandler) != null) {
      throw new IllegalArgumentException("type '" + typeHandler.type.getCanonicalName() + "' already added");
    }
  }

  protected AbstractFastSerializer() {
    for (final TypeHandler typeHandler : TypeHandlers.ALL) {
      addTypeHandler(typeHandler);
    }
  }

  public final List<TypeHandler> typeHandlers() {
    return new ArrayList<>(class2typeHandler.values());
  }

  protected final void fixupFields() {
    for (final TypeHandler typeHandler : class2typeHandler.values()) {
      if (typeHandler instanceof ClassTypeHandler) {
        for (final FieldHandler fieldHandler : ((ClassTypeHandler)typeHandler).fieldHandlers()) {
          fieldHandler.fixup(class2typeHandler);
        }
      }
    }
  }

  @Override public final void write(final Object value, final Writer writer) throws Exception {
    new Output(writer, class2typeHandler).writeWithId(value);
  }

  protected static void checkClass(final Class<?> type) {
    if (Modifier.isAbstract(type.getModifiers())) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is abstract");
    } else if (type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is an enumeration");
    }
  }

  protected static void checkEnum(final Class<?> type) {
    if (!type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is not an enumeration");
    }
  }

  public static List<Field> ownFields(final Class<?> type) {
    final List<Field> fields = new ArrayList<>(16);
    for (final Field field : type.getDeclaredFields()) {
      final int modifiers = field.getModifiers();
      if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
        fields.add(field);
      }
    }
    return fields;
  }

  public static List<Field> allFields(Class<?> type) {
    final List<Field> fields = new ArrayList<>(16);
    while ((type != null) && (type != Throwable.class)) {
      fields.addAll(ownFields(type));
      type = type.getSuperclass();
    }
    return fields;
  }

  public static <E extends Enum<E>> TypeConverter<E, Integer> enumToInteger(final Class<E> enumeration) {
    final E[] constants = enumeration.getEnumConstants();
    return new TypeConverter<E, Integer>(enumeration, Integer.class) {
      @Override public Integer to(final E value) {
        return value.ordinal();
      }
      @Override public E from(final Integer value) {
        return constants[value];
      }
    };
  }

  public final void print(final PrintWriter printer) {
    final List<TypeHandler> typeHandlers = typeHandlers();
    Collections.sort(typeHandlers, new Comparator<TypeHandler>() {
      @Override public int compare(final TypeHandler typeHandler1, final TypeHandler typeHandler2) {
        return Integer.valueOf(typeHandler1.id).compareTo(typeHandler2.id);
      }
    });
    for (final TypeHandler typeHandler : typeHandlers) {
      if (typeHandler.id < TypeHandlers.SIZE) {
        continue;
      }
      printer.print((typeHandler.id - TypeHandlers.SIZE) + ": " + typeHandler.type.getCanonicalName());
      if (typeHandler instanceof ConverterTypeHandler) {
        final ConverterTypeHandler converterTypeHandler = (ConverterTypeHandler)typeHandler;
        if (converterTypeHandler.type.isEnum()) {
          printer.println();
          final Object[] constants = converterTypeHandler.type.getEnumConstants();
          for (int c = 0; c < constants.length; c++) {
            //noinspection rawtypes
            printer.println("  " + c + ": " + ((Enum)constants[c]).name());
          }
        } else {
          printer.println(" -> " + converterTypeHandler.serializableTypeHandler.type.getCanonicalName());
        }
      } else {
        final ClassTypeHandler classTypeHandler = (ClassTypeHandler)typeHandler;
        printer.println(" (referenceable=" + classTypeHandler.referenceable + ')');
        for (final FieldHandler fieldHandler : classTypeHandler.fieldHandlers()) {
          printer.println("  " + (fieldHandler.id - FieldHandler.FIRST_FIELD) + ": " + fieldHandler.field);
        }
      }
      printer.println();
    }
  }

}

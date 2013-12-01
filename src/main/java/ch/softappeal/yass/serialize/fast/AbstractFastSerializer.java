package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.convert.BinaryTypeConverter;
import ch.softappeal.yass.serialize.convert.IntegerTypeConverter;
import ch.softappeal.yass.serialize.convert.LongTypeConverter;
import ch.softappeal.yass.serialize.convert.StringTypeConverter;
import ch.softappeal.yass.serialize.convert.TypeConverter;
import ch.softappeal.yass.util.Reference;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
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
 * <li>enumeration types</li>
 * <li>{@link List} (deserialize creates an {@link ArrayList})</li>
 * <li>class hierarchies with all non-static and non-transient fields</li>
 * <li>exceptions (but without fields of {@link Throwable}; therefore, you should implement {@link Throwable#getMessage()})</li>
 * <li>graphs with cycles</li>
 * </ul>
 */
public abstract class AbstractFastSerializer implements Serializer {

  private final Map<Class<?>, TypeHandler> class2typeHandler = new HashMap<>(16);

  final void addTypeHandler(final TypeHandler typeHandler) {
    if (class2typeHandler.put(typeHandler.type, typeHandler) != null) {
      throw new IllegalArgumentException("type '" + typeHandler.type.getCanonicalName() + "' already added");
    }
  }

  AbstractFastSerializer() {
    for (final TypeHandler typeHandler : TypeHandlers.ALL) {
      addTypeHandler(typeHandler);
    }
  }

  final Collection<TypeHandler> typeHandlers() {
    return new ArrayList<>(class2typeHandler.values());
  }

  final void fixupFields() {
    for (final TypeHandler typeHandler : class2typeHandler.values()) {
      if (typeHandler instanceof ClassTypeHandler) {
        for (final FieldHandler fieldHandler : ((ClassTypeHandler)typeHandler).fieldHandlers) {
          fieldHandler.fixup(class2typeHandler);
        }
      }
    }
  }

  @Override public final void write(final Object value, final Writer writer) throws Exception {
    new Output(writer, class2typeHandler).writeWithId(value);
  }

  static void checkClass(final Class<?> type) {
    if (Modifier.isAbstract(type.getModifiers())) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is abstract");
    } else if (type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is an enumeration");
    }
  }

  static void checkEnum(final Class<?> type) {
    if (!type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is not an enumeration");
    }
  }

  static List<Field> declaredFields(final Class<?> type) {
    final List<Field> fields = new ArrayList<>(16);
    for (final Field field : type.getDeclaredFields()) {
      final int modifiers = field.getModifiers();
      if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
        fields.add(field);
      }
    }
    return fields;
  }

  static TypeHandler typeHandler(final TypeConverter typeConverter, final int id) {
    final Reference<TypeHandler> typeHandler = Reference.create();
    typeConverter.accept(new TypeConverter.Visitor() {

      @Override public void visit(final IntegerTypeConverter typeConverter) {
        typeHandler.set(new TypeHandler(typeConverter.type, id) {
          @Override Object readNoId(final Input input) throws Exception {
            return typeConverter.fromInteger((Integer)TypeHandlers.INTEGER.readNoId(input));
          }
          @Override void writeNoId(final Object value, final Output output) throws Exception {
            TypeHandlers.INTEGER.writeNoId(typeConverter.toInteger(value), output);
          }
        });
      }

      @Override public void visit(final LongTypeConverter typeConverter) {
        typeHandler.set(new TypeHandler(typeConverter.type, id) {
          @Override Object readNoId(final Input input) throws Exception {
            return typeConverter.fromLong((Long)TypeHandlers.LONG.readNoId(input));
          }
          @Override void writeNoId(final Object value, final Output output) throws Exception {
            TypeHandlers.LONG.writeNoId(typeConverter.toLong(value), output);
          }
        });
      }

      @Override public void visit(final StringTypeConverter typeConverter) {
        typeHandler.set(new TypeHandler(typeConverter.type, id) {
          @Override Object readNoId(final Input input) throws Exception {
            return typeConverter.fromString((String)TypeHandlers.STRING.readNoId(input));
          }
          @Override void writeNoId(final Object value, final Output output) throws Exception {
            TypeHandlers.STRING.writeNoId(typeConverter.toString(value), output);
          }
        });
      }

      @Override public void visit(final BinaryTypeConverter typeConverter) {
        typeHandler.set(new TypeHandler(typeConverter.type, id) {
          @Override Object readNoId(final Input input) throws Exception {
            return typeConverter.fromBinary((byte[])TypeHandlers.BYTE_ARRAY.readNoId(input));
          }
          @Override void writeNoId(final Object value, final Output output) throws Exception {
            TypeHandlers.BYTE_ARRAY.writeNoId(typeConverter.toBinary(value), output);
          }
        });
      }

    });
    return typeHandler.get();
  }

  public final void printNumbers(final PrintWriter printer) {
    final List<TypeHandler> typeHandlers = new ArrayList<>(class2typeHandler.values());
    Collections.sort(typeHandlers, new Comparator<TypeHandler>() {
      @Override public int compare(final TypeHandler typeHandler1, final TypeHandler typeHandler2) {
        return Integer.valueOf(typeHandler1.id).compareTo(typeHandler2.id);
      }
    });
    for (final TypeHandler typeHandler : typeHandlers) {
      printer.println((typeHandler.id - TypeHandlers.SIZE) + " (" + typeHandler.id + "): " + typeHandler.type.getCanonicalName());
      if (typeHandler.type.isEnum()) {
        final Object[] constants = typeHandler.type.getEnumConstants();
        for (int c = 0; c < constants.length; c++) {
          //noinspection rawtypes
          printer.println("  " + c + ": " + ((Enum)constants[c]).name());
        }
      } else if (typeHandler instanceof ClassTypeHandler) {
        for (final FieldHandler fieldHandler : ((ClassTypeHandler)typeHandler).fieldHandlers) {
          printer.println("  " + (fieldHandler.id - FieldHandler.FIRST_FIELD) + " (" + fieldHandler.id + "): " + fieldHandler.field);
        }
      }
    }
  }

}

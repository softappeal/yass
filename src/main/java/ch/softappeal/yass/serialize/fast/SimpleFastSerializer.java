package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.convert.IntegerTypeConverter;
import ch.softappeal.yass.serialize.convert.TypeConverter;
import ch.softappeal.yass.serialize.reflect.Reflector;
import ch.softappeal.yass.util.Exceptions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This serializer allocates numbers for type and field id's automatically.
 * The implementation is very efficient because the allocations are consecutive.
 * <p/>
 * There is no support for contract versioning. Therefore, all peers must have the same version of the contract.
 */
public final class SimpleFastSerializer extends AbstractFastSerializer {

  private static ClassTypeHandler classTypeHandler(final Class<?> type, final int id, final Reflector reflector, final boolean referenceable) {
    int fieldId = FieldHandler.FIRST_FIELD;
    final List<FieldHandler> fieldHandlers = new ArrayList<>(16);
    for (Class<?> t = type; (t != null) && (t != Throwable.class); t = t.getSuperclass()) {
      final List<Field> fields = declaredFields(t);
      Collections.sort(fields, new Comparator<Field>() { // guarantees field order
        @Override public int compare(final Field f1, final Field f2) {
          return f1.getName().compareTo(f2.getName());
        }
      });
      for (final Field field : fields) {
        fieldHandlers.add(new FieldHandler(field, fieldId++, reflector.accessor(field)));
      }
    }
    return new ClassTypeHandler(type, id, reflector, referenceable, fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()])) {
      @Override FieldHandler fieldHandler(final int id) {
        return fieldHandlers[id - FieldHandler.FIRST_FIELD];
      }
    };
  }

  private final TypeHandler[] typeHandlers;

  /**
   * @param concreteClasses instances of these classes can only be used in trees
   * @param referenceableConcreteClasses instances of these classes can be used in graphs
   */
  @SuppressWarnings("unchecked")
  public SimpleFastSerializer(
    final Reflector.Factory reflectorFactory,
    final List<TypeConverter> typeConverters,
    final List<Class<?>> enumerations,
    final List<Class<?>> concreteClasses,
    final List<Class<?>> referenceableConcreteClasses
  ) {
    int typeId = TypeHandlers.SIZE;
    for (final TypeConverter typeConverter : typeConverters) {
      addTypeHandler(typeHandler(typeConverter, typeId++));
    }
    for (final Class<?> type : enumerations) {
      checkEnum(type);
      //noinspection rawtypes
      addTypeHandler(typeHandler(IntegerTypeConverter.forEnum((Class<Enum>)type), typeId++));
    }
    try {
      for (final Class<?> type : concreteClasses) {
        checkClass(type);
        addTypeHandler(classTypeHandler(type, typeId++, reflectorFactory.create(type), false));
      }
      for (final Class<?> type : referenceableConcreteClasses) {
        checkClass(type);
        addTypeHandler(classTypeHandler(type, typeId++, reflectorFactory.create(type), true));
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
    final Collection<TypeHandler> handlers = typeHandlers();
    typeHandlers = new TypeHandler[handlers.size()];
    for (final TypeHandler typeHandler : handlers) {
      typeHandlers[typeHandler.id] = typeHandler;
    }
    fixupFields();
  }

  @Override public Object read(final Reader reader) throws Exception {
    return new Input(reader) {
      @Override TypeHandler typeHandler(final int id) {
        return typeHandlers[id];
      }
    }.readWithId();
  }

}

package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This serializer uses numbers for type and field id's from its {@link Tag}.
 * Tag values must be &gt;= 0.
 * Field numbers must be unique in the path to its super classes.
 * <p/>
 * There is support for contract versioning.
 * Deserialization of old classes to new classes with new {@link Nullable} fields is allowed. These fields will be set to {@code null}.
 * Default values for these fields could be implemented with a getter method checking for {@code null}.
 */
public final class TaggedFastSerializer extends AbstractFastSerializer {

  private static void checkId(final int id, final AnnotatedElement element) {
    if (id < 0) {
      throw new IllegalArgumentException("tag '" + id + "' for '" + element + "' must be >= 0");
    }
  }

  private static ClassTypeHandler classTypeHandler(final Class<?> type, final int typeId, final Reflector reflector, final boolean referenceable) {
    final Map<Integer, FieldHandler> id2fieldHandler = new HashMap<>(16);
    for (final Field field : allFields(type)) {
      final int fieldId = Check.hasTag(field);
      checkId(fieldId, field);
      final FieldHandler fieldHandler = new FieldHandler(field, fieldId + FieldHandler.FIRST_FIELD, reflector.accessor(field));
      @Nullable final FieldHandler oldFieldHandler = id2fieldHandler.put(fieldHandler.id, fieldHandler);
      if (oldFieldHandler != null) {
        throw new IllegalArgumentException(
          "field tag '" + fieldId + "' used for '" + oldFieldHandler.field + "' and '" + fieldHandler.field + '\''
        );
      }
    }
    return new ClassTypeHandler(type, typeId, reflector, referenceable, id2fieldHandler.values()) {
      @Override protected FieldHandler fieldHandler(final int id) {
        return id2fieldHandler.get(id);
      }
    };
  }

  private final Map<Integer, TypeHandler> id2typeHandler;

  private static int typeId(final Class<?> type) {
    final int id = Check.hasTag(type);
    checkId(id, type);
    return id + TypeHandlers.SIZE;
  }

  /**
   * @param concreteClasses instances of these classes can only be used in trees
   * @param referenceableConcreteClasses instances of these classes can be used in graphs
   */
  @SuppressWarnings("unchecked")
  public TaggedFastSerializer(
    final Reflector.Factory reflectorFactory,
    final Collection<TypeConverterId> typeConverterIds,
    final Collection<Class<?>> enumerations,
    final Collection<Class<?>> concreteClasses,
    final Collection<Class<?>> referenceableConcreteClasses
  ) {
    for (final TypeConverterId typeConverterId : typeConverterIds) {
      checkId(typeConverterId.id, typeConverterId.typeConverter.type);
      addTypeHandler(new ConverterTypeHandler(typeConverterId.typeConverter, typeConverterId.id + TypeHandlers.SIZE, this));
    }
    for (final Class<?> type : enumerations) {
      checkEnum(type);
      addTypeHandler(new ConverterTypeHandler(enumToInteger((Class) type), typeId(type), this));
    }
    try {
      for (final Class<?> type : concreteClasses) {
        checkClass(type);
        addTypeHandler(classTypeHandler(type, typeId(type), reflectorFactory.create(type), false));
      }
      for (final Class<?> type : referenceableConcreteClasses) {
        checkClass(type);
        addTypeHandler(classTypeHandler(type, typeId(type), reflectorFactory.create(type), true));
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
    final Collection<TypeHandler> typeHandlers = typeHandlers();
    id2typeHandler = new HashMap<>(typeHandlers.size());
    for (final TypeHandler typeHandler : typeHandlers) {
      @Nullable final TypeHandler oldTypeHandler = id2typeHandler.put(typeHandler.id, typeHandler);
      if (oldTypeHandler != null) {
        throw new IllegalArgumentException(
          "type tag '" + (typeHandler.id - TypeHandlers.SIZE) + "' used for '" + oldTypeHandler.type.getCanonicalName() + "' and '" + typeHandler.type.getCanonicalName() + '\''
        );
      }
    }
    fixupFields();
  }

  @Override public Object read(final Reader reader) throws Exception {
    return new Input(reader) {
      @Override protected TypeHandler typeHandler(final int id) {
        return id2typeHandler.get(id);
      }
    }.readWithId();
  }

}

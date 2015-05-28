package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Reflector;

/**
 * This is the Java implementation of the yass JavaScript serializer.
 * Only the following base types are allowed: {@link Boolean}, {@link Double}, {@link String} and byte[].
 */
public abstract class AbstractJsFastSerializer extends AbstractFastSerializer {

    public static final TypeDesc BOOLEAN_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID, BaseTypeHandlers.BOOLEAN);
    public static final TypeDesc DOUBLE_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 1, BaseTypeHandlers.DOUBLE);
    public static final TypeDesc STRING_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 2, BaseTypeHandlers.STRING);
    public static final TypeDesc BYTES_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 3, BaseTypeHandlers.BYTE_ARRAY);
    public static final int FIRST_ID = TypeDesc.FIRST_ID + 4;

    protected AbstractJsFastSerializer(final Reflector.Factory reflectorFactory) {
        super(reflectorFactory);
        addBaseType(BOOLEAN_TYPEDESC);
        addBaseType(DOUBLE_TYPEDESC);
        addBaseType(STRING_TYPEDESC);
        addBaseType(BYTES_TYPEDESC);
    }

}

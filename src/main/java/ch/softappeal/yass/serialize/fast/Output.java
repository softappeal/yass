package ch.softappeal.yass.serialize.fast;

import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Nullable;

import java.util.List;
import java.util.Map;

final class Output {

    final Writer writer;
    private final Map<Class<?>, TypeDesc> class2typeDesc;
    @Nullable Map<Object, Integer> object2reference;

    Output(final Writer writer, final Map<Class<?>, TypeDesc> class2typeDesc) {
        this.writer = writer;
        this.class2typeDesc = class2typeDesc;
    }

    void write(final @Nullable Object value) throws Exception {
        if (value == null) {
            TypeDesc.NULL.write(null, this);
        } else if (value instanceof List) {
            TypeDesc.LIST.write(value, this);
        } else {
            final @Nullable TypeDesc typeDesc = class2typeDesc.get(value.getClass());
            if (typeDesc == null) {
                throw new IllegalArgumentException("missing type '" + value.getClass().getCanonicalName() + '\'');
            }
            typeDesc.write(value, this);
        }
    }

}

package ch.softappeal.yass.serialize.test;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
abstract class FastSerializerSpec {

    abstract void writeVarInt(int value);

    abstract class TypeHandler {
        int id;
        abstract void writeBody(Object value);
        void writeIdAndBody(Object value) {
            writeVarInt(id);
            writeBody(value);
        }
    }

    TypeHandler NULL = new TypeHandler(/* 0, Void.class */) {
        @Override void writeBody(Object value) {
            // empty
        }
    };

    TypeHandler REFERENCE = new TypeHandler(/* 1, Reference.class */) {
        @Override void writeBody(Object value) {
            writeVarInt((Integer)value);
        }
    };

    TypeHandler LIST = new TypeHandler(/* 2, List.class */) {
        @Override void writeBody(Object value) {
            var list = (List<Object>)value;
            writeVarInt(list.size());
            list.forEach(FastSerializerSpec.this::writeObject);
        }
    };

    Map<Class<?>, TypeHandler> class2typeHandler;

    void writeObject(Object value) {
        if (value == null) {
            NULL.writeIdAndBody(null);
        } else if (value instanceof List) {
            LIST.writeIdAndBody(value);
        } else {
            class2typeHandler.get(value.getClass()).writeIdAndBody(value);
        }
    }

    Map<Object, Integer> object2reference = new IdentityHashMap<>();

    class ClassTypeHandler extends TypeHandler {
        boolean referenceable;
        List<FieldHandler> fieldHandlers;
        @Override void writeBody(Object value) {
            fieldHandlers.forEach(fieldHandler -> fieldHandler.write(value));
            writeVarInt(0);
        }
        @Override void writeIdAndBody(Object value) {
            if (referenceable) {
                var reference = object2reference.get(value);
                if (reference != null) {
                    REFERENCE.writeIdAndBody(reference);
                    return;
                }
                object2reference.put(value, object2reference.size());
            }
            super.writeIdAndBody(value);
        }
    }

    abstract Object getFieldValue(Object object);

    class FieldHandler {
        int id;
        TypeHandler typeHandler; // null if ClassTypeHandler or type not in class2typeHandler
        void write(Object object) {
            var fieldValue = getFieldValue(object);
            if (fieldValue != null) {
                writeVarInt(id);
                if (typeHandler == null) {
                    writeObject(fieldValue);
                } else {
                    typeHandler.writeBody(fieldValue);
                }
            }
        }
    }

}

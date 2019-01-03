package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.*;
import ch.softappeal.yass.serialize.fast.*;

import static ch.softappeal.yass.serialize.fast.BaseTypeSerializersKt.*;

/**
 * Shows how to use a contract internal base type.
 */
public final class Expiration {

    public final int year;

    public Expiration(final int year) {
        this.year = year;
    }

    public static final BaseTypeSerializer<?> TYPE_SERIALIZER = new BaseTypeSerializer<Expiration>(Expiration.class, getIntSerializer().getFieldType()) {
        @Override
        public Expiration read(final Reader reader) {
            return new Expiration(
                getIntSerializer().read(reader)
            );
        }

        @Override
        public void write(final Writer writer, final Expiration value) {
            getIntSerializer().write(writer, value.year);
        }
    };

}

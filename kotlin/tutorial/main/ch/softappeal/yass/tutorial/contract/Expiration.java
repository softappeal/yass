package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeSerializer;

import static ch.softappeal.yass.serialize.fast.Kt.getIntSerializer;

/**
 * Shows how to use a contract internal base type.
 */
public final class Expiration {

    public final int year;
    public final int month;
    public final int day;

    public Expiration(final int year, final int month, final int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static final BaseTypeSerializer<?> TYPE_SERIALIZER = new BaseTypeSerializer<Expiration>(Expiration.class) {
        @Override public Expiration read(final Reader reader) {
            return new Expiration(
                getIntSerializer().read(reader),
                getIntSerializer().read(reader),
                getIntSerializer().read(reader)
            );
        }
        @Override public void write(final Writer writer, final Expiration value) {
            getIntSerializer().write(writer, value.year);
            getIntSerializer().write(writer, value.month);
            getIntSerializer().write(writer, value.day);
        }
    };

}

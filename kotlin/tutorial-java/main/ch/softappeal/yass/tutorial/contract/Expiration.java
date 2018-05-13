package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;

import static ch.softappeal.yass.serialize.fast.BaseTypeHandlersKt.getBTH_INTEGER;

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

    public static final BaseTypeHandler<?> TYPE_HANDLER = new BaseTypeHandler<Expiration>(Expiration.class) {
        @Override public Expiration read(final Reader reader) {
            return new Expiration(
                getBTH_INTEGER().read(reader),
                getBTH_INTEGER().read(reader),
                getBTH_INTEGER().read(reader)
            );
        }
        @Override public void write(final Writer writer, final Expiration value) {
            getBTH_INTEGER().write(writer, value.year);
            getBTH_INTEGER().write(writer, value.month);
            getBTH_INTEGER().write(writer, value.day);
        }
    };

}

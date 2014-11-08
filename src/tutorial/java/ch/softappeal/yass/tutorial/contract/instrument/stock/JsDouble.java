package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;

/**
 * Shows how to use a new base type.
 */
public final class JsDouble {

    public final double d;

    public JsDouble(final double d) {
        this.d = d;
    }

    public static final BaseTypeHandler<?> TYPE_HANDLER = new BaseTypeHandler<JsDouble>(JsDouble.class) {
        @Override public JsDouble read(final Reader reader) throws Exception {
            return new JsDouble(BaseTypeHandlers.DOUBLE.read(reader));
        }
        @Override public void write(final JsDouble value, final Writer writer) throws Exception {
            BaseTypeHandlers.DOUBLE.write(value.d, writer);
        }
    };

}

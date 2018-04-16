package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.serialize.CompositeSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

public final class PrinterSerializer extends CompositeSerializer {

    public PrinterSerializer(final Serializer serializer) {
        super(serializer);
    }

    @Override public @Nullable Object read(final Reader reader) throws Exception {
        final var s = new StringBuilder("read(");
        final var value = serializer.read(new Reader() {
            @Override public byte readByte() throws Exception {
                final var value = reader.readByte();
                s.append(' ').append(value);
                return value;
            }
            @Override public void readBytes(final byte[] buffer, int offset, int length) throws Exception {
                while (length-- > 0) {
                    buffer[offset++] = readByte();
                }
            }
        });
        s.append(" )");
        System.out.println(s);
        return value;
    }

    @Override public void write(final @Nullable Object value, final Writer writer) throws Exception {
        final var s = new StringBuilder("write(");
        serializer.write(value, new Writer() {
            @Override public void writeByte(final byte value) throws Exception {
                s.append(' ').append(value);
                writer.writeByte(value);
            }
            @Override public void writeBytes(final byte[] buffer, int offset, int length) throws Exception {
                while (length-- > 0) {
                    writeByte(buffer[offset++]);
                }
            }
        });
        s.append(" )");
        System.out.println(s);
    }

}

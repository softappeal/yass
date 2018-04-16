package ch.softappeal.yass.transport;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.serialize.CompositeSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

/**
 * Adds a frame start byte for easy synchronization.
 */
public final class FrameSerializer extends CompositeSerializer {

    public FrameSerializer(final Serializer serializer) {
        super(serializer);
    }

    private static final byte START = (byte)0xFF;
    private static final byte QUOTE = (byte)0xFE;
    private static final byte QUOTE_START = 1;
    private static final byte QUOTE_QUOTE = 0;

    @Override public void write(final @Nullable Object value, final Writer writer) throws Exception {
        writer.writeByte(START);
        serializer.write(value, new Writer() {
            @Override public void writeByte(final byte value) throws Exception {
                if (value == START) {
                    writer.writeByte(QUOTE);
                    writer.writeByte(QUOTE_START);
                } else if (value == QUOTE) {
                    writer.writeByte(QUOTE);
                    writer.writeByte(QUOTE_QUOTE);
                } else {
                    writer.writeByte(value);
                }
            }
            @Override public void writeBytes(final byte[] buffer, int offset, int length) throws Exception {
                while (length-- > 0) {
                    writeByte(buffer[offset++]);
                }
            }
        });
    }

    @Override public @Nullable Object read(final Reader reader) throws Exception {
        if (reader.readByte() != START) {
            throw new IllegalStateException("missing frame start");
        }
        return serializer.read(new Reader() {
            @Override public byte readByte() throws Exception {
                final var value = reader.readByte();
                if (value == START) {
                    throw new IllegalStateException("unexpected frame start");
                }
                if (value == QUOTE) {
                    final var quotedValue = reader.readByte();
                    if (quotedValue == QUOTE_START) {
                        return START;
                    } else if (quotedValue == QUOTE_QUOTE) {
                        return QUOTE;
                    }
                    throw new IllegalStateException("unexpected quoted value");
                }
                return value;
            }
            @Override public void readBytes(final byte[] buffer, int offset, int length) throws Exception {
                while (length-- > 0) {
                    buffer[offset++] = readByte();
                }
            }
        });
    }

}

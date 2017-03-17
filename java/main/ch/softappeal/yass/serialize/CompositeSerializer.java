package ch.softappeal.yass.serialize;

import java.util.Objects;

public abstract class CompositeSerializer implements Serializer {

    protected final Serializer serializer;

    protected CompositeSerializer(final Serializer serializer) {
        this.serializer = Objects.requireNonNull(serializer);
    }

}

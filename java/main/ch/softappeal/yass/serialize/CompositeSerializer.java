package ch.softappeal.yass.serialize;

import ch.softappeal.yass.util.Check;

public abstract class CompositeSerializer implements Serializer {

    protected final Serializer serializer;

    protected CompositeSerializer(final Serializer serializer) {
        this.serializer = Check.notNull(serializer);
    }

}

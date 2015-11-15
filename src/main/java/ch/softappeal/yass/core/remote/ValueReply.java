package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

public final class ValueReply extends Reply {

    private static final long serialVersionUID = 1L;

    public final @Nullable Object value;

    public ValueReply(final @Nullable Object value) {
        this.value = value;
    }

    @Override @Nullable Object process() {
        return value;
    }

}

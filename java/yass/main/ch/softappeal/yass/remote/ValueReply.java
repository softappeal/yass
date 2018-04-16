package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

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

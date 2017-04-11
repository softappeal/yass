package ch.softappeal.yass.core.remote;

import java.util.Objects;

public final class ExceptionReply extends Reply {

    private static final long serialVersionUID = 1L;

    public final Exception exception;

    public ExceptionReply(final Exception exception) {
        this.exception = Objects.requireNonNull(exception);
    }

    @Override Object process() throws Exception {
        throw exception;
    }

}

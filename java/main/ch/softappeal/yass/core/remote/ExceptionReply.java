package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

public final class ExceptionReply extends Reply {

    private static final long serialVersionUID = 1L;

    public final Exception exception;

    public ExceptionReply(final Exception exception) {
        this.exception = Check.notNull(exception);
    }

    @Override Object process() throws Exception {
        throw exception;
    }

}

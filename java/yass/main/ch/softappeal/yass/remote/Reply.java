package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

public abstract class Reply extends Message {

    private static final long serialVersionUID = 1L;

    Reply() {
        // empty
    }

    abstract @Nullable Object process() throws Exception;

}

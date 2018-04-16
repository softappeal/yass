package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

public class ReplyVisibility {

    public static @Nullable Object process(final Reply reply) throws Exception {
        return reply.process();
    }

}

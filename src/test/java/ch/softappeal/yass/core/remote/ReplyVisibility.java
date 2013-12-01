package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Nullable;

public class ReplyVisibility {

  @Nullable public static Object process(final Reply reply) throws Throwable {
    return reply.process();
  }

}

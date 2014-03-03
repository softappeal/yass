package ch.softappeal.yass.tutorial.client;

import io.undertow.servlet.api.ThreadSetupAction;
import io.undertow.servlet.core.CompositeThreadSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.nio.ByteBuffer;
import java.util.Collections;

public final class UndertowClient {

  public static void main(final String... args) throws Exception {
    final XnioWorker worker = Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
    final Pool<ByteBuffer> buffers = new ByteBufferSlicePool(1024, 10240);
    final ServerWebSocketContainer container = new ServerWebSocketContainer(
      DefaultClassIntrospector.INSTANCE, worker, buffers, new CompositeThreadSetupAction(Collections.<ThreadSetupAction>emptyList()), true
    );
    JettyClient.run(container);
  }

}

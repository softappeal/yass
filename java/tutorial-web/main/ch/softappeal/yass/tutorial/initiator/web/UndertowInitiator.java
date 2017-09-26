package ch.softappeal.yass.tutorial.initiator.web;

import io.undertow.server.XnioByteBufferPool;
import io.undertow.servlet.core.ContextClassLoaderSetupAction;
import io.undertow.servlet.util.DefaultClassIntrospector;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import org.xnio.ByteBufferSlicePool;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import java.util.List;

public final class UndertowInitiator extends WebInitiatorSetup {

    public static void main(final String... args) throws Exception {
        run(new ServerWebSocketContainer(
            DefaultClassIntrospector.INSTANCE,
            Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
            new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
            List.of(new ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
            true,
            true
        ));
    }

}

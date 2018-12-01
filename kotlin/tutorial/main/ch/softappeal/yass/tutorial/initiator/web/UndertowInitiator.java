package ch.softappeal.yass.tutorial.initiator.web;

import io.undertow.server.*;
import io.undertow.servlet.core.*;
import io.undertow.servlet.util.*;
import io.undertow.websockets.jsr.*;
import org.xnio.*;

import java.util.*;

public final class UndertowInitiator extends WebInitiatorSetup {

    public static void main(final String... args) throws Exception {
        run(new ServerWebSocketContainer(
            DefaultClassIntrospector.INSTANCE,
            Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true)),
            new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
            Collections.singletonList(new ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
            true,
            true
        ));
    }

}

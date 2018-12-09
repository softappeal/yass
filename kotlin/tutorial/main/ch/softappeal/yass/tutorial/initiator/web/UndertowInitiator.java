package ch.softappeal.yass.tutorial.initiator.web;

import io.undertow.server.*;
import io.undertow.servlet.core.*;
import io.undertow.servlet.util.*;
import io.undertow.websockets.jsr.*;
import org.xnio.*;

import java.io.*;
import java.util.*;

public final class UndertowInitiator extends WebInitiatorSetup {

    public static void main(final String... args) throws Exception {
        run(new ServerWebSocketContainer(
            DefaultClassIntrospector.INSTANCE,
            () -> {
                try {
                    return Xnio.getInstance().createWorker(OptionMap.create(Options.THREAD_DAEMON, true));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            new XnioByteBufferPool(new ByteBufferSlicePool(1024, 10240)),
            Collections.singletonList(new ContextClassLoaderSetupAction(ClassLoader.getSystemClassLoader())),
            true,
            true
        ));
    }

}

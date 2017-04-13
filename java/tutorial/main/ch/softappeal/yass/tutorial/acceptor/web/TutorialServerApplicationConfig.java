package ch.softappeal.yass.tutorial.acceptor.web;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.Set;

public final class TutorialServerApplicationConfig implements ServerApplicationConfig {

    @Override public Set<ServerEndpointConfig> getEndpointConfigs(final Set<Class<? extends Endpoint>> endpointClasses) {
        return Collections.singleton(WebAcceptorSetup.ENDPOINT_CONFIG);
    }

    @Override public Set<Class<?>> getAnnotatedEndpointClasses(final Set<Class<?>> scanned) {
        return Collections.emptySet();
    }

}

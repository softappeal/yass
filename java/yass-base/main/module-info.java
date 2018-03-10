module ch.softappeal.yass.base {
    exports ch.softappeal.yass.core;
    exports ch.softappeal.yass.core.remote;
    exports ch.softappeal.yass.core.remote.session;
    exports ch.softappeal.yass.serialize;
    exports ch.softappeal.yass.serialize.fast;
    exports ch.softappeal.yass.transport;
    exports ch.softappeal.yass.transport.socket;
    exports ch.softappeal.yass.util;
    requires jdk.unsupported;
}

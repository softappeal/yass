package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;

import java.util.concurrent.Executor;

public abstract class AcceptorSetup {

    protected static TransportSetup createTransportSetup(final Executor dispatchExecutor) {
        return TransportSetup.ofContractSerializer(Config.SERIALIZER, connection -> new AcceptorSession(connection, dispatchExecutor));
    }

}

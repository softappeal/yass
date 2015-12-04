package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.transport.TransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;

public abstract class InitiatorSetup {

    protected static TransportSetup createTransportSetup(final SessionFactory sessionFactory) {
        return TransportSetup.ofContractSerializer(Config.SERIALIZER, sessionFactory);
    }

}

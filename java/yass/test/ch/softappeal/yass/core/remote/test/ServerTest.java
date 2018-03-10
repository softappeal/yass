package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ServerTest {

    static final Client CLIENT = new Client() {
        @Override public void invoke(final Client.Invocation invocation) throws Exception {
            invocation.invoke(
                false,
                request -> new Server(ContractIdTest.ID.service(new InvokeTest.TestServiceImpl()))
                    .invocation(false, request).invoke(invocation::settle)
            );
        }
    };

    @Test public void duplicatedService() {
        final var service = ContractIdTest.ID.service(new InvokeTest.TestServiceImpl());
        try {
            new Server(service, service);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("serviceId 987654 already added", e.getMessage());
        }
    }

    @Test public void noService() {
        try {
            CLIENT.proxy(ContractId.create(InvokeTest.TestService.class, 123456, TaggedMethodMapper.FACTORY)).nothing();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("no serviceId 123456 found (methodId 0)", e.getMessage());
        }
    }

}

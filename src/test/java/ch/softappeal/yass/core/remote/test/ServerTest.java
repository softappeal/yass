package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.Tunnel;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ServerTest {

    static final Client client = new Client() {
        @Override public Object invoke(final Client.Invocation invocation) throws Exception {
            return invocation.invoke(new Tunnel() {
                @Override public Reply invoke(final Request request) throws Exception {
                    return new Server(ContractIdTest.ID.service(new InvokeTest.TestServiceImpl())).invocation(request).invoke();
                }
            });
        }
    };

    @Test public void duplicatedService() {
        final Service service = ContractIdTest.ID.service(new InvokeTest.TestServiceImpl());
        try {
            new Server(service, service);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("serviceId 987654 already added", e.getMessage());
        }
    }

    @Test public void noService() {
        try {
            client.proxy(ContractId.create(InvokeTest.TestService.class, 123456, TaggedMethodMapper.FACTORY)).nothing();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("no serviceId 123456 found (methodId 0)", e.getMessage());
        }
    }

}

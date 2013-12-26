package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ServerTest {

  static final Client client = new Server(TaggedMethodMapper.FACTORY, ContractIdTest.ID.service(new InvokeTest.TestServiceImpl())).client;

  @Test public void duplicatedService() {
    final Service service = ContractIdTest.ID.service(new InvokeTest.TestServiceImpl());
    try {
      new Server(TaggedMethodMapper.FACTORY, service, service);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("serviceId 'TestService' already added", e.getMessage());
    }
  }

  @Test public void noService() {
    try {
      ContractId.create(InvokeTest.TestService.class, "xxx").invoker(client).proxy().nothing();
      Assert.fail();
    } catch (final RuntimeException e) {
      Assert.assertEquals("no serviceId 'xxx' found (methodId '0')", e.getMessage());
    }
  }

}

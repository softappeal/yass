package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ContractIdTest {

  public static final ContractId<InvokeTest.TestService> ID = ContractId.create(InvokeTest.TestService.class, "TestService");

  @Test public void test() {
    Assert.assertSame(InvokeTest.TestService.class, ID.contract);
    Assert.assertEquals("TestService", ID.id);
  }

}

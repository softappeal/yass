package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.remote.ContractId;
import ch.softappeal.yass.remote.TaggedMethodMapper;
import ch.softappeal.yass.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ContractIdTest {

    public static final ContractId<InvokeTest.TestService> ID = ContractId.create(InvokeTest.TestService.class, 987654, TaggedMethodMapper.FACTORY);

    @Test public void test() {
        Assert.assertSame(InvokeTest.TestService.class, ID.contract);
        Assert.assertEquals(987654, ID.id);
    }

}

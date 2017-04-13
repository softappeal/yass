package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class ContractIdTest {

    public static final ContractId<InvokeTest.TestService> ID = ContractId.create(InvokeTest.TestService.class, 987654, TaggedMethodMapper.FACTORY);

    @Test public void test() {
        Assert.assertSame(InvokeTest.TestService.class, ID.contract);
        Assert.assertEquals(987654, ID.id);
    }

}

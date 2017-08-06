package ch.softappeal.yass.util.unsupported.test;

import ch.softappeal.yass.util.unsupported.UnsupportedInstantiators;
import org.junit.Assert;
import org.junit.Test;

public class UnsupportedInstantiatorsTest {

    @Test public void noDefaultConstructor() {
        final NoDefaultConstructor instance = (NoDefaultConstructor)UnsupportedInstantiators.UNSAFE.apply(NoDefaultConstructor.class).get();
        Assert.assertTrue(instance.i == 0);
    }

}

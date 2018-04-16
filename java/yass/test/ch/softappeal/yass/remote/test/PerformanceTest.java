package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.test.InvokeTest;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PerformanceTest extends InvokeTest {

    @Test public void test() {
        ch.softappeal.yass.test.PerformanceTest.task(
            ServerTest.CLIENT.proxy(ContractIdTest.ID)
        ).run(1_000, TimeUnit.NANOSECONDS);
    }

}

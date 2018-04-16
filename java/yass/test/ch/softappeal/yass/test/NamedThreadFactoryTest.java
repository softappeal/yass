package ch.softappeal.yass.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NamedThreadFactoryTest {

    @Test public void test() throws Exception {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
        executor.execute(() -> {
            throw new RuntimeException("run");
        });
        TimeUnit.MILLISECONDS.sleep(100L);
        executor.shutdown();
    }

}
